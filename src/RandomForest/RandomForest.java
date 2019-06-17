/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package RandomForest;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import static java.lang.Double.parseDouble;
import static java.lang.Math.max;
import static java.lang.Math.round;
import static java.lang.Math.sqrt;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;
import java.util.TreeMap;
import javafx.util.Pair;

/**
 *
 * @author Jakub
 */
public class RandomForest {
    private String[] targetValues;    //wartości klasy target
    private ArrayList<Pair<String,String[]>> fileData;  //dane z pliku
    private ArrayList<Pair<String,String[]>>[] trainingSamples;  //próbki treningowe
    private ArrayList<Pair<String,String[]>>[] testSamples;  //próbki testowe
    private ArrayList<BinaryTree> forest[]; //las przygotowany na każdym zbiorze treningowym
    
    private String[] variablesNames;  //nazwy cech
    private String positiveClass;   //wartość klasy target uznawana za pozytywną
    private boolean[] numericalVariables;  //flaga wartości liczbowych jako typ wartości cechy
    private int nTree; //ilość drzew dla każdej próbki treningowej
    private int nFold; //ilość testów walidacyjnych
    private int maxDepth;   //max głębokość drzewa
    private Long seed;  //ziarno losowe lasu
    private Random random;  //zmienna losowa lasu
    private int mTry;   //ilość cech na drzewo
    private double errorRate;
    private double accuracy;
    private double sensitivity;
    private double specificity;
    
    public RandomForest(){
        targetValues=null;
        fileData=null;
        trainingSamples=null;
        testSamples=null;
        forest=null;
        variablesNames=null;
        positiveClass=null;
        numericalVariables=null;
        nTree=100;
        nFold=10;
        maxDepth=Integer.MAX_VALUE;
        seed=System.currentTimeMillis();
        random=new Random(seed);
        mTry=1;
        errorRate=0;
        accuracy=0;
        sensitivity=0;
        specificity=0;
    }
    
    
    public void setMaxDepth(int maxDepth){
        this.maxDepth=max(1,maxDepth);
    }
    
    public void setSeed(long seed){
        this.seed=seed;
        random=new Random(seed);
    }
    
    public void loadFile(String filename, String splitBy, boolean varNames,Integer targetIndex, String positiveClass, String missingValues){
        Scanner scanner=null;
        try {
            if(!filename.contains(".csv") && !filename.contains(".txt")){
                System.out.println("ERROR: bad file type");
                System.exit(0);
            }
            scanner=new Scanner(new File(Paths.get(System.getProperty("user.home"),"Documents\\RFDatasets",filename).toString()));
        }
        catch(FileNotFoundException ex) {
            System.out.println("ERROR: file not found. Check if you have deleted the file \""
                    +filename+ "\" from your \"documents\\RFDatasets\" folder");
            System.exit(0);
        }
        if(varNames){
            String line=scanner.nextLine();
            if(line==null)
                return;
            line=line.replace("\"","");
            String[] tempLine=line.split(splitBy);
            variablesNames=new String[tempLine.length-1];
            if(targetIndex==null)
                targetIndex=tempLine.length-1;
            for(int i=0,j=0;i<variablesNames.length;i++,j++){
                if(j==targetIndex)
                    j++;
                variablesNames[i]=tempLine[j];
            }
        }
        fileData=new ArrayList<Pair<String,String[]>>();
        boolean missingData=false;  //flaga brakujących wartości
        TreeMap<String,Integer> target=null;
        HashMap<String,HashMap<String,Integer>>[] varAttributesSpread=null;
        
        while(scanner.hasNext()){
            String line=scanner.nextLine();
            line=line.replace("\"","");
            String[] tempInstance=line.split(",");
            String[] instanceToAdd=new String[tempInstance.length-1];
            if(targetIndex==null)
                targetIndex=tempInstance.length-1;
            //przygotowanie odpowiedniej struktury rekordu do bazy
            for(int variableID=0,tempVariableID=0;variableID<instanceToAdd.length;variableID++,tempVariableID++){
               if(tempVariableID==targetIndex)
                   tempVariableID++;
               instanceToAdd[variableID]=tempInstance[tempVariableID];
            }
            Pair<String,String[]> instance=new Pair(tempInstance[targetIndex],instanceToAdd);
            
            if(variablesNames==null){
                variablesNames=new String[instanceToAdd.length];
                for(int i=0;i<variablesNames.length;i++)
                    variablesNames[i]="var"+i;
            }
            if(numericalVariables==null){
                numericalVariables=new boolean[instanceToAdd.length];
                for(int i=0;i<numericalVariables.length;i++)
                    numericalVariables[i]=true;
            }
            if(target==null || varAttributesSpread==null){
                //mapa ilości wartości atrybutów klasy target
                target=new TreeMap<String,Integer>();
                //tablica dla każdej cechy hashMapy target<hashmapa wystąpień atrybutów>
                varAttributesSpread=new HashMap[variablesNames.length];
                for(int i=0;i<variablesNames.length;i++)
                    varAttributesSpread[i]=new HashMap<String,HashMap<String,Integer>>();
            }
        //aktualizacja stanu mapy rozkładu targeta
            if(target.containsKey(instance.getKey())){
                Integer count=target.get(instance.getKey());
                target.put(instance.getKey(),count+1);
            }else target.put(instance.getKey(),1);
            //analiza rekordu i aktualizacja map wartości
            for(int varID=0;varID<variablesNames.length;varID++){
                //ignorujemy puste rekordy, ale flagujemy problem
                if(instance.getValue()[varID].equals(missingValues)){
                    missingData=true;
                    continue;
                }
                //ustalamy rodzaj wartości danej cechy, jeżeli choć jeden element nie jest liczbowy, cała cecha nie jest
                if(numericalVariables[varID]==true){
                    if(instanceToAdd[varID].equals(missingValues))
                        break;
                    try{ double val=parseDouble(instanceToAdd[varID]);}
                    catch(NumberFormatException e){ numericalVariables[varID]=false;}
                }
                //aktualizujemy mapy wartości
                if(varAttributesSpread[varID].containsKey(instance.getKey())){
                    HashMap<String,Integer> attributeSpread=varAttributesSpread[varID].get(instance.getKey());
                    if(attributeSpread.containsKey(instance.getValue()[varID])){
                        Integer count=attributeSpread.get(instance.getValue()[varID]);
                        attributeSpread.put(instance.getValue()[varID], count+1);
                    }
                    else attributeSpread.put(instance.getValue()[varID], 1);
                }
                else{
                    HashMap<String,Integer> attributeSpread=new HashMap<String,Integer>();
                    attributeSpread.put(instance.getValue()[varID],1);
                    varAttributesSpread[varID].put(instance.getKey(),attributeSpread);
                }
            }
            fileData.add(instance);
        }
        scanner.close();
        //kontrola poprawności klasy pozytywnej
        if(!target.containsKey(positiveClass)){
            System.out.println("Error: positive value of a class variable not found.");
            System.exit(0);
        }
        this.positiveClass=positiveClass;
        targetValues=new String[target.keySet().size()];
        target.keySet().toArray(targetValues);
        
        //jeżeli znaleziono brakujące pola
        if(missingData){
            //ustalanie najlepszych atrybutów każdej cechy w zależności od klasy
            ArrayList<String[]> bestAtts=new ArrayList<String[]>();
            ArrayList<String>targetClasses=new ArrayList<String>();
            for(String key: target.keySet())
                targetClasses.add(key);
            for(String targetClass: targetClasses){
                String[] bestTargetAttributes=new String[variablesNames.length];
                for(int varID=0;varID<variablesNames.length;varID++){
                    if(numericalVariables[varID]){
                        double average=0;
                        int total=0;
                        for(Map.Entry<String,Integer> attribute: varAttributesSpread[varID].get(targetClass).entrySet()){
                            average+=parseDouble(attribute.getKey())*attribute.getValue();
                            total+=attribute.getValue();
                        }
                        average/=total;
                        average=round(average*1000d)/1000d;
                        bestTargetAttributes[varID]=""+average;
                    }
                    else{
                        String bestAtt="";
                        int bestAttCount=0;
                        for(Map.Entry<String,Integer> attribute: varAttributesSpread[varID].get(targetClass).entrySet()){
                            if(attribute.getValue()>bestAttCount){
                                bestAttCount=attribute.getValue();
                                bestAtt=attribute.getKey();
                            }
                        }
                        bestTargetAttributes[varID]=bestAtt;
                    }
                }
                bestAtts.add(bestTargetAttributes);
            }
            //uzupełnianie brakujących rekordów
            for(Pair<String,String[]> instance: fileData)
                for(int varID=0;varID<instance.getValue().length;varID++)
                    if(instance.getValue()[varID].equals(missingValues))
                        instance.getValue()[varID]=bestAtts.get(targetClasses.indexOf(instance.getKey()))[varID];
        }
    }
    
    /**
     * przygotowuje próbki testowe i treningowe dla lasu
     * kFold: crosswalidacja validationNValue podZbiorów
     * holdOut zbiór testowy to validationNValue% zbioru
     * leaveOneOut: każda instancja zbioru jest osobnym zbiorem testowym (zasobo- i czasowo-)chłonna
     * @param validationType
     * @param validationNValue 
     */
    public void prepareValidationSamples(){
        ArrayList<Pair<String,String[]>> copiedData;
        trainingSamples=new ArrayList[nFold];
        testSamples=new ArrayList[nFold];
        for(int i=0;i<nFold;i++){
            trainingSamples[i]=new ArrayList<Pair<String,String[]>>();
            testSamples[i]=new ArrayList<Pair<String,String[]>>();
        }
        int foldID=0;
        copiedData=new ArrayList<Pair<String,String[]>>(fileData);
        while(!copiedData.isEmpty()){
            int instanceID=random.nextInt(copiedData.size());
            testSamples[foldID].add(copiedData.get(instanceID));
            for(int notID=0;notID<nFold;notID++){
                if(notID!=foldID)
                    trainingSamples[notID].add(copiedData.get(instanceID));
            }
            copiedData.remove(instanceID);
            foldID=(foldID==nFold-1)?0:foldID+1;
        }
    }
    
    /**
     * przygotowuje sadzonki drzew gotowych do rozrostu przy pomocy ilości okreslonych metryk
     * @param nTree ilość drzew
     * @param mTry ilośc cech branych pod uwagę
     */
    public void seedForest(Integer nTree,Integer mTry){
        if(variablesNames==null){
            System.out.println("Error. data not loaded");
            System.exit(0);
        }
        if(mTry==null){ //jeśli nie dano nulla i załadowano plik
            this.mTry=(int)sqrt(variablesNames.length);
        }else if(mTry>0 && mTry<=variablesNames.length){  //jeśli 
            this.mTry=mTry;
        }else{
            System.out.println("WARNING: Bad mTry value, default sqrt() value will be used");
            this.mTry=(int)sqrt(variablesNames.length);
        }
        if(nTree==null)
            nTree=100;
        forest=new ArrayList[nFold];
        this.nTree=nTree;
        HashMap<Integer,Integer> bagInstances=new HashMap<Integer,Integer>();
        for(int foldID=0;foldID<nFold;foldID++){
            forest[foldID]=new ArrayList<BinaryTree>();
            for(int treeID=0;treeID<nTree;treeID++){
                bagInstances.clear();
                for(int i=0;i<trainingSamples[foldID].size();i++){
                    int instanceID=random.nextInt(trainingSamples[foldID].size());
                    if(bagInstances.containsKey(instanceID)){
                        Integer count = bagInstances.get(instanceID);
                        bagInstances.put(instanceID,count+1);
                    }
                    else bagInstances.put(instanceID,1);
                }
                ArrayList<Pair<String,String[]>> bag=new ArrayList<Pair<String,String[]>>();
                ArrayList<Pair<String,String[]>> oob=new ArrayList<Pair<String,String[]>>();
                for(int instanceID=0;instanceID<trainingSamples[foldID].size();instanceID++){
                    if(bagInstances.containsKey(instanceID)){
                        for(int i=0;i<bagInstances.get(instanceID);i++)
                            bag.add(trainingSamples[foldID].get(instanceID));
                    }
                    else oob.add(trainingSamples[foldID].get(instanceID));
                }
                forest[foldID].add(new BinaryTree(bag,oob,numericalVariables,random.nextLong(),this.mTry));
            }
        }
    }
    
    /**
     * dokonuje rozrostu drzew w lesie przy użyciu wskazanego kryterium podziału bez konieczności budowania zbiorów na nowo
     * @param splitCriterion 
     */
    public void growForest(SplitCriterion splitCriterion){
        for(ArrayList<BinaryTree> subForest: forest){
            for(BinaryTree tree: subForest){
                tree.regrow(splitCriterion,maxDepth);
            }
        }
    }
    
    public void evaluate(int mTree,VoteType voteType){
        if(mTree>=nTree)
            mTree=nTree;
        double TP=0;
        double FP=0;
        double TN=0;
        double FN=0;
        for(int testID=0;testID<nFold;testID++){
            if(voteType==VoteType.WEIGHTED){//jeżeli las jest ważony, wypada ustalić wagi drzew(w przeciwnym wypadku są równe 1)
                for(BinaryTree tree: forest[testID])
                    tree.setWeight();   //ustawia wagę na równą skuteczności drzewa
            }
            else{
                for(BinaryTree tree: forest[testID])
                    tree.resetWeight(); //ustawia wagę na 1
            }
            for(Pair<String,String[]> instance: testSamples[testID]){
                HashMap<String,Double> votes=new HashMap<String,Double>();    //mapa głosów
                for(int treeID=0;treeID<mTree;treeID++){
                    String treeVote=forest[testID].get(treeID).vote(instance.getValue());
                    if(votes.containsKey(treeVote)){
                        double count=votes.get(treeVote);
                        votes.put(treeVote,count+forest[testID].get(treeID).getWeight());
                    }
                    else votes.put(treeVote,forest[testID].get(treeID).getWeight());
                }
                String forestVote="";
                double highestVote=0;
                for(String currentVote: votes.keySet()){
                    if(highestVote<votes.get(currentVote)){
                        highestVote=votes.get(currentVote);
                        forestVote=currentVote;
                    }
                }
                int instanceClassID=-1;
                int forestVoteClassID=-1;
                for(int keyID=0;keyID<targetValues.length;keyID++){
                    if(targetValues[keyID].equals(instance.getKey()))
                        instanceClassID=keyID;
                    if(targetValues[keyID].equals(forestVote))
                        forestVoteClassID=keyID;
                }
                
                if(targetValues[instanceClassID].equals(positiveClass)){
                    if(instanceClassID==forestVoteClassID)
                        TP++;
                    else FN++;
                }
                else{
                    if(instanceClassID==forestVoteClassID)
                        TN++;
                    else FP++;
                }
            }
        }
        accuracy=(TP+TN)/(TP+TN+FP+FN);
        errorRate=(FN+FP)/(TP+TN+FP+FN);
        specificity=TN/(TN+FP);
        sensitivity=TP/(TP+FN);
        errorRate=round(errorRate*10000)/10000d;
        accuracy=round(accuracy*10000)/10000d;
        specificity=round(specificity*10000)/10000d;
        sensitivity=round(sensitivity*10000)/10000d;
    }
    
    public double getAccuracy(){
        return accuracy;
    }
    public double getErrorRate(){
        return errorRate;
    }
    public double getSpecificity(){
        return specificity;
    }
    public double getSensitivity(){
        return sensitivity;
    }
    
}
