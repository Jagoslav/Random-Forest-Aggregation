/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package RandomForest;

import static java.lang.Double.MAX_VALUE;
import static java.lang.Double.parseDouble;
import static java.lang.Math.abs;
import static java.lang.Math.log10;
import static java.lang.Math.round;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import javafx.util.Pair;

/**
 *
 * @author Jakub
 */
public class Node {
    private ArrayList<Pair<String,String[]>> nodeData;  //dane węzła
    private boolean[] numericalVariables;   //tablica wartości liczbowych
    private boolean[] usedVariables;    //tablica używanych cech
    private int mTry;   //ilość cech na węzeł
    private Random random;
    private int depth;
    
    private HashMap<String,Integer> targetClassSpread;
    private HashMap<String,HashMap<String,Integer>>[] attributeSpread;
    
    private Node leftChild; //podwęzeł nr1
    private Node rightChild;    //podwęzeł nr2
    private String nodeClass;   //klasa zwracana przez węzeł, jeśli jest liściem
    
    private int splittingVariableID;
    private String splittingAttribute;
    public Node(
            ArrayList<Pair<String,String[]>> nodeData, boolean[] numericalVariables,
            SplitCriterion splitCriterion,int depth,int mTry,long seed){
        this.nodeData=nodeData;
        this.numericalVariables=numericalVariables;
        this.mTry=mTry;
        this.random=new Random(seed);
        this.targetClassSpread=null;
        this.attributeSpread=null;
        this.depth=depth;
        
        leftChild=null;
        rightChild=null;
        nodeClass=null;
        splittingVariableID=-1;
        splittingAttribute=null;
        this.usedVariables=new boolean[numericalVariables.length];
        for(int i=0;i<usedVariables.length;i++)
            usedVariables[i]=false;
        for(int i=0;i<mTry;i++){
            int varID=0;
            do{varID=random.nextInt(numericalVariables.length);}
            while(usedVariables[varID]==true);
            usedVariables[varID]=true;
        }
        //ustalanie ilości wystąpień atrybutów cech zbioru
        targetClassSpread=new HashMap<String,Integer>();
        attributeSpread=new HashMap[numericalVariables.length];
        for(int i=0;i<numericalVariables.length;i++){
            attributeSpread[i]=new HashMap<String,HashMap<String,Integer>>();
        }
        for(Pair<String,String[]> instance: nodeData){
            //aktualizowanie rozkładu wartości klasy target
            if(targetClassSpread.containsKey(instance.getKey())){
                Integer count=targetClassSpread.get(instance.getKey());
                targetClassSpread.put(instance.getKey(),count+1);
            }
            else targetClassSpread.put(instance.getKey(),1);
            for(int varID=0;varID<instance.getValue().length;varID++){
            //aktualizowanie rozkładu wartości cech w zależności od targeta)
                //jeśli atrybut jest w bazie
                if(attributeSpread[varID].containsKey(instance.getValue()[varID])){
                    if(attributeSpread[varID].get(instance.getValue()[varID]).containsKey(instance.getKey())){
                        Integer count=attributeSpread[varID].get(instance.getValue()[varID]).get(instance.getKey());
                        attributeSpread[varID].get(instance.getValue()[varID]).put(instance.getKey(),count+1);
                    }
                    else attributeSpread[varID].get(instance.getValue()[varID]).put(instance.getKey(),1);
                }
                //jeśli go nie ma
                else{
                    HashMap<String,Integer> targetSpread=new HashMap<String,Integer>();
                    targetSpread.put(instance.getKey(),1);
                    attributeSpread[varID].put(instance.getValue()[varID], targetSpread);
                }
            }
        }
        if(depth<=0){
            int bestCount=0;
            for(String target: targetClassSpread.keySet()){
                if(targetClassSpread.get(target)>bestCount){
                    bestCount=targetClassSpread.get(target);
                    nodeClass=target;
                }
            }
            return;
        }
        boolean splitPossible=false;
        if(splitCriterion==SplitCriterion.GDI){
            double giniIndex=1;
            for(String target: targetClassSpread.keySet()){
                double proportion=targetClassSpread.get(target)/(double)nodeData.size();
                giniIndex-=proportion*proportion;
            }
            double bestVariableGain=-MAX_VALUE;
            splittingVariableID=-1;
            splittingAttribute="";
            for(int varID=0;varID<usedVariables.length;varID++){
                if(!usedVariables[varID])
                    continue;
                double variableGain;
                String bestVariableAttribute;
                if(numericalVariables[varID]){
                    double[] attributes=new double[attributeSpread[varID].keySet().size()];
                    int attributeID=0;
                    for(String attribute: attributeSpread[varID].keySet()){
                        attributes[attributeID]=parseDouble(attribute);
                        attributeID++;
                    }
                    double[] partitions=new double[attributeSpread[varID].keySet().size()+1];
                    partitions[0]=attributes[0]-0.5;
                    partitions[partitions.length-1]=attributes[attributes.length-1]+0.5;
                    for(int i=1;i<attributes.length;i++)
                        partitions[i]=(attributes[i-1]+attributes[i])/2d;

                    variableGain=-MAX_VALUE;
                    int bestPartitionID=-1;
                    for(int partitionID=0;partitionID<partitions.length;partitionID++){
                        double partitionGain=giniIndex;
                        int smallerAttributesCount=0;
                        int biggerAttributesCount=0;
                        for(String attribute: attributeSpread[varID].keySet()){
                            for(Map.Entry<String,Integer> targetSpread: attributeSpread[varID].get(attribute).entrySet()){
                                if(parseDouble(attribute)<partitions[partitionID]){
                                    smallerAttributesCount+=targetSpread.getValue();
                                }
                                else biggerAttributesCount+=targetSpread.getValue();
                            }
                        }
                        double leftGini=1;
                        double rightGini=1;
                        for(String target: targetClassSpread.keySet()){
                            int leftTargetCount=0;
                            int rightTargetCount=0;
                            for(String attribute: attributeSpread[varID].keySet()){
                                if(!attributeSpread[varID].get(attribute).containsKey(target))
                                    continue;
                                if(parseDouble(attribute)<partitions[partitionID]){
                                    leftTargetCount+=attributeSpread[varID].get(attribute).get(target);
                                }
                                else rightTargetCount+=attributeSpread[varID].get(attribute).get(target);
                            }
                            double leftProportion=(smallerAttributesCount==0)?0:(leftTargetCount/(double)smallerAttributesCount);
                            double rightProportion=(biggerAttributesCount==0)?0:(rightTargetCount/(double)biggerAttributesCount);
                            leftGini-=leftProportion*leftProportion;
                            rightGini-=rightProportion*rightProportion;
                        }
                        partitionGain-=(smallerAttributesCount/(double)nodeData.size())*leftGini;
                        partitionGain-=(biggerAttributesCount/(double)nodeData.size())*rightGini;
                        if(partitionGain>variableGain){
                            variableGain=partitionGain;
                            bestPartitionID=partitionID;
                        }
                    }
                    bestVariableAttribute=partitions[bestPartitionID]+"";
                }
                else{
                    double bestGini=-MAX_VALUE;
                    bestVariableAttribute="";
                    variableGain=giniIndex;
                    for(String attribute: attributeSpread[varID].keySet()){
                        int attributeCount=0;
                        for(Map.Entry<String,Integer> targetSpread: attributeSpread[varID].get(attribute).entrySet()){
                            attributeCount+=targetSpread.getValue();
                        }
                        double attributeGini=1;
                        for(Map.Entry<String,Integer> targetSpread: attributeSpread[varID].get(attribute).entrySet()){
                            double proportion=targetSpread.getValue()/(double)attributeCount;
                            attributeGini-=proportion*proportion;
                        }
                        if(bestGini<attributeGini){
                            bestGini=attributeGini;
                            bestVariableAttribute=attribute;
                        }
                        variableGain-=(attributeCount/(double)nodeData.size())*attributeGini;
                    }
                }
                if(variableGain>bestVariableGain){
                    bestVariableGain=variableGain;
                    splittingAttribute=bestVariableAttribute;
                    splittingVariableID=varID;
                }
            }

            if(splittingVariableID<0 || splittingVariableID>=numericalVariables.length || bestVariableGain<=0){
                int bestCount=0;
                for(String target: targetClassSpread.keySet()){
                    if(targetClassSpread.get(target)>bestCount){
                        bestCount=targetClassSpread.get(target);
                        nodeClass=target;
                    }
                }
                splitPossible=false;
            }
            else splitPossible=true;
        }
        else if(splitCriterion==SplitCriterion.DEVIANCE){
            double entropy=0;
            for(String target: targetClassSpread.keySet()){
                double proportion=targetClassSpread.get(target)/(double)nodeData.size();
                entropy-=proportion*(log10(proportion)/log10(2));
            }
            double bestInfoGain=-MAX_VALUE;
            splittingVariableID=-1;
            splittingAttribute="";
            for(int varID=0;varID<usedVariables.length;varID++){
                if(!usedVariables[varID])
                    continue;
                double variableInfoGain;
                String bestVariableAttribute;
                if(numericalVariables[varID]){
                    double[] attributes=new double[attributeSpread[varID].keySet().size()];
                    int attributeID=0;
                    for(String attribute: attributeSpread[varID].keySet()){
                        attributes[attributeID]=parseDouble(attribute);
                        attributeID++;
                    }
                    double[] partitions=new double[attributeSpread[varID].keySet().size()+1];
                    partitions[0]=attributes[0]-0.5;
                    partitions[partitions.length-1]=attributes[attributes.length-1]+0.5;
                    for(int i=1;i<attributes.length;i++)
                        partitions[i]=(attributes[i-1]+attributes[i])/2d;

                    variableInfoGain=-MAX_VALUE;
                    int bestPartitionID=-1;
                    for(int partitionID=0;partitionID<partitions.length;partitionID++){
                        double partitionGain=entropy;
                        int smallerAttributesCount=0;
                        int biggerAttributesCount=0;
                        for(String attribute: attributeSpread[varID].keySet()){
                            for(Map.Entry<String,Integer> targetSpread: attributeSpread[varID].get(attribute).entrySet()){
                                if(parseDouble(attribute)<partitions[partitionID]){
                                    smallerAttributesCount+=targetSpread.getValue();
                                }
                                else biggerAttributesCount+=targetSpread.getValue();
                            }
                        }
                        double leftEntropy=0;
                        double rightEntropy=0;
                        for(String target: targetClassSpread.keySet()){
                            int leftTargetCount=0;
                            int rightTargetCount=0;
                            for(String attribute: attributeSpread[varID].keySet()){
                                if(!attributeSpread[varID].get(attribute).containsKey(target))
                                    continue;
                                if(parseDouble(attribute)<partitions[partitionID]){
                                    leftTargetCount+=attributeSpread[varID].get(attribute).get(target);
                                }
                                else rightTargetCount+=attributeSpread[varID].get(attribute).get(target);
                            }
                            double leftProportion=(smallerAttributesCount==0)?0:(leftTargetCount/(double)smallerAttributesCount);
                            double rightProportion=(biggerAttributesCount==0)?0:(rightTargetCount/(double)biggerAttributesCount);
                            leftEntropy-=(leftProportion==0)?0:leftProportion*(log10(leftProportion)/log10(2));
                            rightEntropy-=(rightProportion==0)?0:rightProportion*(log10(rightProportion)/log10(2));
                        }
                        partitionGain-=(smallerAttributesCount/(double)nodeData.size())*leftEntropy;
                        partitionGain-=(biggerAttributesCount/(double)nodeData.size())*rightEntropy;
                        if(partitionGain>variableInfoGain){
                            variableInfoGain=partitionGain;
                            bestPartitionID=partitionID;
                        }
                    }
                    bestVariableAttribute=partitions[bestPartitionID]+"";
                }
                else{
                    double bestGini=-MAX_VALUE;
                    bestVariableAttribute="";
                    variableInfoGain=entropy;
                    for(String attribute: attributeSpread[varID].keySet()){
                        int attributeCount=0;
                        for(Map.Entry<String,Integer> targetSpread: attributeSpread[varID].get(attribute).entrySet()){
                            attributeCount+=targetSpread.getValue();
                        }
                        double attributeEntropy=0;
                        for(Map.Entry<String,Integer> targetSpread: attributeSpread[varID].get(attribute).entrySet()){
                            double proportion=targetSpread.getValue()/(double)attributeCount;
                            attributeEntropy-=proportion*(log10(proportion)/log10(2));;
                        }
                        if(bestGini<attributeEntropy){
                            bestGini=attributeEntropy;
                            bestVariableAttribute=attribute;
                        }
                        variableInfoGain-=(attributeCount/(double)nodeData.size())*attributeEntropy;
                    }
                }
                if(variableInfoGain>bestInfoGain){
                    bestInfoGain=variableInfoGain;
                    splittingAttribute=bestVariableAttribute;
                    splittingVariableID=varID;
                }
            }
            if(splittingVariableID<0 || splittingVariableID>=numericalVariables.length || bestInfoGain<=0){
                int bestCount=0;
                for(String target: targetClassSpread.keySet()){
                    if(targetClassSpread.get(target)>bestCount){
                        bestCount=targetClassSpread.get(target);
                        nodeClass=target;
                    }
                }
                splitPossible=false;
            }
            else splitPossible= true;
        }
        else if(splitCriterion==SplitCriterion.TWOING){
            double bestCriterion=-MAX_VALUE;
            for(int varID=0;varID<usedVariables.length;varID++){
                if(numericalVariables[varID]){
                    double[] attributes=new double[attributeSpread[varID].keySet().size()];
                    int attributeID=0;
                    for(String attribute: attributeSpread[varID].keySet()){
                        attributes[attributeID]=parseDouble(attribute);
                        attributeID++;
                    }
                    double[] partitions=new double[attributeSpread[varID].keySet().size()+1];
                    partitions[0]=attributes[0]-0.5;
                    partitions[partitions.length-1]=attributes[attributes.length-1]+0.5;
                    for(int i=1;i<attributes.length;i++)
                        partitions[i]=(attributes[i-1]+attributes[i])/2d;
                    int bestPartitionID=-1;
                    double bestPartitionCriterion=-MAX_VALUE;
                    for(int partitionID=0;partitionID<partitions.length;partitionID++){
                        int smallerCount=0;
                        int biggerCount=0;
                        for(String attribute: attributeSpread[varID].keySet()){
                            int count=0;
                            for(String target: attributeSpread[varID].get(attribute).keySet())
                                count+=attributeSpread[varID].get(attribute).get(target);
                            if(parseDouble(attribute)<partitions[partitionID])
                                smallerCount+=count;
                            else biggerCount+=count;
                        }
                        double partitionCriterion=(smallerCount/(double)nodeData.size())*(biggerCount/(double)nodeData.size());
                        double targetCriterionSum=0;
                        for(String target:targetClassSpread.keySet()){
                            double smallerAttProp=0;
                            double biggerAttProp=0;
                            for(String consideredAttribute: attributeSpread[varID].keySet()){
                                if(attributeSpread[varID].get(consideredAttribute).containsKey(target)){
                                    if(parseDouble(consideredAttribute)<partitions[partitionID])
                                        smallerAttProp+=attributeSpread[varID].get(consideredAttribute).get(target);
                                    else biggerAttProp+=attributeSpread[varID].get(consideredAttribute).get(target);
                                }
                            }
                            smallerAttProp/=targetClassSpread.get(target);
                            biggerAttProp/=targetClassSpread.get(target);
                            targetCriterionSum+=abs(smallerAttProp-biggerAttProp);
                        }
                        partitionCriterion*=targetCriterionSum;
                        if(bestPartitionCriterion<partitionCriterion){
                            bestPartitionCriterion=partitionCriterion;
                            bestPartitionID=partitionID;
                        }
                    }
                    if(bestCriterion<bestPartitionCriterion){
                        splittingVariableID=varID;
                        splittingAttribute=partitions[bestPartitionID]+"";
                        bestCriterion=bestPartitionCriterion;
                    }
                }
                else{
                    double bestAttributeCriterion=-MAX_VALUE;
                    String bestVarAttribute=null;
                    for(String attribute: attributeSpread[varID].keySet()){
                        int attCount=0;
                        for(Map.Entry<String,Integer> target: attributeSpread[varID].get(attribute).entrySet()){
                            attCount+=target.getValue();
                        }
                        int notAttCount=nodeData.size()-attCount;
                        double attributeCriterion=(attCount/(double)nodeData.size())*(notAttCount/(double)nodeData.size());
                        double targetCriterionSum=0;
                        for(String target:targetClassSpread.keySet()){
                            double targetAtt=0;
                            if(attributeSpread[varID].get(attribute).containsKey(target))
                                targetAtt=attributeSpread[varID].get(attribute).get(target)/(double)nodeData.size();
                            double targetNotAtt=0;
                            if(attributeSpread[varID].get(attribute).containsKey(target))
                                targetNotAtt=(targetClassSpread.get(target)-attributeSpread[varID].get(attribute).get(target))/(double)nodeData.size();
                            targetCriterionSum+=abs(targetAtt-targetNotAtt);
                        }
                        attributeCriterion*=targetCriterionSum;
                        if(bestAttributeCriterion<attributeCriterion){
                            bestAttributeCriterion=attributeCriterion;
                            bestVarAttribute=attribute;
                        }
                    }
                    if(bestCriterion<bestAttributeCriterion){
                        splittingVariableID=varID;
                        splittingAttribute=bestVarAttribute;
                        bestCriterion=bestAttributeCriterion;
                    }
                }
            }
            if(splittingVariableID<0 || splittingVariableID>=numericalVariables.length || bestCriterion<=0){
                int bestCount=0;
                for(String target: targetClassSpread.keySet()){
                    if(targetClassSpread.get(target)>bestCount){
                        bestCount=targetClassSpread.get(target);
                        nodeClass=target;
                    }
                }
                splitPossible=false;
            }
            else splitPossible=true;
        }
        else{
            System.out.println("CRITICAL ERROR: unknown split criterion");
            System.exit(0);
        }

        targetClassSpread=null;
        for(int varID=0;varID<attributeSpread.length;varID++){
            for(String attribute: attributeSpread[varID].keySet()){
                attributeSpread[varID]=null;
            }
        }
        attributeSpread=null;
        if(splitPossible)
            split(splitCriterion, depth-1);
    }
    /**
     * metoda odpowiedzialna za utworzenie podwęzłów aktywnego węzła
     * @param splitCriterion
     * @param maxDepth 
     */
    private void split(SplitCriterion splitCriterion,int maxDepth){
        ArrayList<Pair<String,String[]>> leftNodeData=new ArrayList<Pair<String,String[]>>();
        ArrayList<Pair<String,String[]>> rightNodeData=new ArrayList<Pair<String,String[]>>();
        for(Pair<String,String[]> instance: nodeData){
            if(numericalVariables[splittingVariableID]){
                if(parseDouble(instance.getValue()[splittingVariableID])<parseDouble(splittingAttribute))
                    leftNodeData.add(instance);
                else rightNodeData.add(instance);
            }
            else if(instance.getValue()[splittingVariableID].equals(splittingAttribute))
                    leftNodeData.add(instance);
                else rightNodeData.add(instance);
        }
        if(leftNodeData.isEmpty())
            leftChild=null;
        else leftChild=new Node(leftNodeData,numericalVariables,splitCriterion,depth-1,mTry,random.nextLong());
        if(rightNodeData.isEmpty())
            rightChild=null;
        else rightChild=new Node(rightNodeData,numericalVariables,splitCriterion,depth-1,mTry,random.nextLong());
        
        
    }
    
    /**
     * metoda odpowiedzialna za podejmowanie decyzji drzewa o zwracanej klasie
     * @param instance
     * @return 
     */
    public String vote(String[] instance){
        if(leftChild==null && rightChild==null)
            return nodeClass;
        else if(numericalVariables[splittingVariableID]){
            if(parseDouble(instance[splittingVariableID])<=parseDouble(splittingAttribute))
                return leftChild.vote(instance);
            else return rightChild.vote(instance);
        }
        else{
            if(instance[splittingVariableID].equals(splittingAttribute))
                return leftChild.vote(instance);
            else return rightChild.vote(instance);
        }
    }
}