/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package RandomForest;

import java.util.ArrayList;
import java.util.Random;
import javafx.util.Pair;

/**
 *
 * @author Jakub
 */
public class BinaryTree {
    ArrayList<Pair<String,String[]>> bag;   //zbiór instancji służących do budowy drzewa
    ArrayList<Pair<String,String[]>> oob;   //zbiór testowy dla danego drzewa
    boolean[] numericalVariables;   //flagi cech posiadających wartości liczbowe
    boolean[] variablesUsed;    //cechy używane do dokonywania podziałów w danym drzewie
    double predictionRate;    //ustalana na podstawie out of bagu skuteczność drzewa
    double treeWeight;  //waga drzewa
    Long seed;  //ziarno drzewa;
    Random random;
    int mTry;   //ilość cech na węzeł
    Node root;  //korzeń drzewa
    
    /**
     * konstruktor obiektu, nie skutkuje rozrostem drzewa
     * @param bag
     * @param oob
     * @param numericalVariables
     * @param variablesUsed 
     */
    public BinaryTree(
            ArrayList<Pair<String,String[]>> bag, ArrayList<Pair<String,String[]>> oob,
            boolean[] numericalVariables,long seed, int mTry){
        this.bag=bag;
        this.oob=oob;
        this.numericalVariables=numericalVariables;
        this.seed=seed;
        this.mTry=mTry;
        predictionRate=1d;
    };
    
    /**
     * właściwa metoda odpowiadająca za rośnięcie drzewa
     * @param splitCriterion
     * @param maxDepth 
     */
    public void regrow(SplitCriterion splitCriterion,int maxDepth){
        random=new Random(seed);
        predictionRate=1d;  //resetowanie wagi drzewa
        root=new Node(bag,numericalVariables,splitCriterion,maxDepth,mTry,random.nextLong());
    }
    
    /**
     * głosowanie drzewa na podstawie danej instancji
     * @param instance
     * @return 
     */
    public String vote(String[] instance){
        return root.vote(instance);
    }
    
    /**
     * metoda służąca do ustalenia wagi danego drzewa dla głosowania ważonego
     */
    public void calculatePredictionRate(){
        int correctCount=0;
        for(Pair<String,String[]> instance: oob){
            if(instance.getKey().equals(root.vote(instance.getValue())))
                correctCount++;
        }
        predictionRate=correctCount/(double)oob.size();
        
    }
    
    public void setWeight(){
        calculatePredictionRate();
        treeWeight=predictionRate;
    };
    public void resetWeight(){
        treeWeight=1;
    }
    public double getWeight(){
        return treeWeight;
    }
    public double getPredictionRate(){
        return predictionRate;
    }
    
}
