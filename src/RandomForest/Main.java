/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package RandomForest;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import static java.lang.Integer.parseInt;
import static java.lang.Math.round;
import java.nio.file.Paths;
import java.util.Scanner;

/**
 *
 * @author Jakub
 */
enum VoteType{MAJORITY,WEIGHTED};
enum SplitCriterion{GDI,DEVIANCE,TWOING}
enum AppAction{CALCULATE_ERROR_RATES,COMPARE_METRICS,ALL};
public class Main {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        String filename=null;    //nazwa pliku z bazą danych
        String splitBy=null;
        Boolean variablesNames=null;   //flaga nazw cech w pierwszej linii tekstu
        Integer targetIndex=null;    //indeks klasy target
        String positiveClass=null;   //wartość klasy pozytywnej
        Integer nTest=null; //ilość przeprowadzanych testów
        Integer nTree=null; //ilość drzew na test
        Integer depthLimit=null; //ograniczenie głębokości drzew
        AppAction appAction=AppAction.ALL;
        Integer mTry=null;   //ilość cech na drzewo
        boolean errorOccured=false;
        String missingValues=null;
        boolean help=false;
        if(args.length==0)
            help=true;
        if(args.length>0){
            for(String parameter: args){
                if(parameter.contains("help"))
                    help=true;
                else if(parameter.contains("filename="))
                    filename=parameter.replace("filename=","");
                else if(parameter.contains("splitBy="))
                    splitBy=parameter.replace("splitBy=","");
                else if(parameter.contains("positive="))
                    positiveClass=parameter.replace("positive=","");
                else if(parameter.contains("names=")){
                    String temp=parameter.replace("names=","");
                    if(temp.equals("true"))
                        variablesNames=true;
                    else if(temp.equals("false"))
                        variablesNames=false;
                    else{
                        System.out.println("Error: invalid input: names");
                        errorOccured=true;  
                    }
                }
                else if(parameter.contains("targetID=")){
                    String tempString=parameter.replace("targetID=","");
                    try{targetIndex=parseInt(tempString);}
                    catch(NumberFormatException e){
                        System.out.println("Error: invalid input: targetID");
                        errorOccured=true;  
                    }
                }
                else if(parameter.contains("nTree=")){
                    String tempString=parameter.replace("nTree=","");
                    try{nTree=parseInt(tempString);}
                    catch(NumberFormatException e){
                        System.out.println("Error: invalid input: nTree");
                        errorOccured=true;  
                    }
                }
                else if(parameter.contains("nTest=")){
                    String tempString=parameter.replace("nTest=","");
                    try{nTest=parseInt(tempString);}
                    catch(NumberFormatException e){
                        System.out.println("Error: invalid input: nTest");
                        errorOccured=true;  
                    }
                }
                else if(parameter.contains("depthLimit=")){
                    String tempString=parameter.replace("depthLimit=","");
                    try{depthLimit=parseInt(tempString);}
                    catch(NumberFormatException e){
                        System.out.println("Error: invalid input: depthLimit");
                        errorOccured=true;
                    }
                }
                else if(parameter.contains("action=")){
                    String tempString=parameter.replace("action=","");
                    if(tempString.equals("all"))
                        appAction=AppAction.ALL;
                    else if(tempString.equals("errorRates"))
                        appAction=AppAction.CALCULATE_ERROR_RATES;
                    else if(tempString.equals("compare"))
                        appAction=AppAction.COMPARE_METRICS;
                    else{
                        System.out.println("Error: invalid input: action");
                        errorOccured=true;
                    }
                }
                else if(parameter.contains("missingValues="))
                    missingValues=parameter.replace("missingValues=","");
                else{
                    System.out.println("ERROR: unknown parameter. Run program without parameters, or use parameter \"help\" to se the manual");
                    errorOccured=true;
                }
            }
        }
        if(help){
            System.out.println("-=Manual=-");
            System.out.println("Use parameters listed below by writing \"parameterName\"=\"value\"");
            System.out.println("Parameters required to run the program:");
            System.out.println("\tfilename      - used to specify the name of the file to use");
            System.out.println("\tsplitBy       - a sequence of characters separating 2 values in a data set");
            System.out.println("\tnames         - used to mark if variables has their names specified in a 1st row of a file\n\t\t\tby true-false value");
            System.out.println("\tpositive      - sets a given class name to be considered as \"positive\"");
            System.out.println("");
            System.out.println("Parameters not required to run the program:");
            System.out.println("\tmissingValues - used to specify how missing values are represented in a file");
            System.out.println("\tnTest         - determines the number of tests that will be made to calculate the average scores");
            System.out.println("\tnTree         - specifies the number of trees that will be build in a single test case");
            System.out.println("\tmTry          - used to determine the number of variables to consider at each split");
            System.out.println("\taction        - choose between 3 modes(errorRates, compare, all)\n\t\t\tto get average error rates, compare metrics or to do both");
            System.exit(0);
        }
        
        if(filename==null){
            System.out.println("ERROR: Database filename not specified");
            errorOccured=true;
        }else try{
            if(filename!=null){
            Scanner scanner=null;
            if(!filename.contains(".csv") && !filename.contains(".txt")){
                System.out.println("ERROR: bad file type");

            }
            else scanner=new Scanner(new File(Paths.get(System.getProperty("user.home"),"Documents\\RFDatasets",filename).toString()));
            scanner.close();
            }
        }
        catch(FileNotFoundException ex) {
            System.out.println("ERROR: file not found. Check if you have deleted the file \""
                    +filename+ "\" from your \"documents\\RFDatasets\" folder");
            errorOccured=true;
        }
        if(splitBy==null){
            System.out.println("ERROR: Unknown data separator");
            errorOccured=true;
        }
        if(variablesNames==null){
            System.out.println("ERROR: field 'variable names' must be specified");
            errorOccured=true;
        }
        if(positiveClass==null){
            System.out.println("ERROR: positive value of a class variable must be specified.");
            errorOccured=true;
        }
        if(errorOccured){
            System.out.println("--==Following errors have occured. Run stopped==--");
            System.out.println("Use parameter \"help\" or run the program without any parameters to get help");
            System.exit(0);
        }

        System.out.println("Application will run using considered field values:");
        System.out.println("\tfilename: "+filename);
        System.out.println("\tdata split by: "+splitBy);
        System.out.println("\tfile contains variables names: "+variablesNames);
        System.out.println("\tclass Index: "+((targetIndex==null)?"Default(last column)":targetIndex));
        System.out.println("\tclass attribute considered positive: "+positiveClass);
        System.out.println("\tnumber of tests performed: "+((nTest==null)?"Default(10)":nTest));
        System.out.println("\tsize of each forest: "+((nTree==null)?"Default(100)":nTree));
        System.out.println("\tlimit for a depth of a tree: "+((depthLimit==null)?"Default(none)":depthLimit));
        System.out.println("\tvariables considered at each split: "+((mTry==null)?"Default(sqrt(variables count))":mTry));
        System.out.println("\tactions performed : "+((appAction==AppAction.ALL)?"Default(Everything)":appAction));
        System.out.println("\tmissing values : "+((missingValues==null)?"none":("signed as -"+missingValues)));
        if(nTest==null)
            nTest=10;
        if(nTree==null)
            nTree=100;
        if(depthLimit==null)
            depthLimit=Integer.MAX_VALUE;

        try{
            if(appAction==AppAction.COMPARE_METRICS|| appAction==AppAction.ALL){
                System.out.println("\n--==Calculating forest performance with different metrics==--");
                RandomForest RF=new RandomForest();
                RF.setMaxDepth(depthLimit);
                RF.loadFile(filename, splitBy, variablesNames, targetIndex,positiveClass,missingValues);

                double[] averageAccuracy=new double[6];
                double[] averageSensitivity=new double[6];
                double[] averageSpecificity=new double[6];
                for(int i=0;i<6;i++){
                    averageAccuracy[i]=0;
                    averageSensitivity[i]=0;
                    averageSpecificity[i]=0;
                }
                try {
                    PrintWriter printWriterGDI;
                    PrintWriter printWriterDEV;
                    PrintWriter printWriterTWO;
                    String filename2=filename.replace(".txt","");
                    filename2=filename.replace(".csv","");
                    printWriterGDI=new PrintWriter(new File(Paths.get(System.getProperty("user.home"),"Documents\\RFDatasets",filename2+"-logiGDI.txt").toString()));
                    printWriterDEV=new PrintWriter(new File(Paths.get(System.getProperty("user.home"),"Documents\\RFDatasets",filename2+"-logiDEV.txt").toString()));
                    printWriterTWO=new PrintWriter(new File(Paths.get(System.getProperty("user.home"),"Documents\\RFDatasets",filename2+"-logiTWO.txt").toString()));
                    for(int testID=0;testID<nTest;testID++){
                        System.out.print("\r\t-Calculating forest prediction: ["+testID+"/"+nTest+"]");
                        RF.prepareValidationSamples();
                        RF.seedForest(nTree,mTry);

                        RF.growForest(SplitCriterion.GDI);
                        RF.evaluate(nTree,VoteType.MAJORITY);
                            averageAccuracy[0]+=RF.getAccuracy();
                            averageSensitivity[0]+=RF.getSensitivity();
                            averageSpecificity[0]+=RF.getSpecificity();
                            printWriterGDI.println((testID+1)+":\tMajority: "+RF.getAccuracy()+"\t"+RF.getSensitivity()+"\t"+RF.getSpecificity());
                        RF.evaluate(nTree,VoteType.WEIGHTED);
                            averageAccuracy[1]+=RF.getAccuracy();
                            averageSensitivity[1]+=RF.getSensitivity();
                            averageSpecificity[1]+=RF.getSpecificity();
                            printWriterGDI.println((testID+1)+":\tWeighted: "+RF.getAccuracy()+"\t"+RF.getSensitivity()+"\t"+RF.getSpecificity());
                        RF.growForest(SplitCriterion.DEVIANCE);
                        RF.evaluate(nTree,VoteType.MAJORITY);
                            averageAccuracy[2]+=RF.getAccuracy();
                            averageSensitivity[2]+=RF.getSensitivity();
                            averageSpecificity[2]+=RF.getSpecificity();
                            printWriterDEV.println((testID+1)+":\tMajority: "+RF.getAccuracy()+"\t"+RF.getSensitivity()+"\t"+RF.getSpecificity());
                        RF.evaluate(nTree,VoteType.WEIGHTED);
                            averageAccuracy[3]+=RF.getAccuracy();
                            averageSensitivity[3]+=RF.getSensitivity();
                            averageSpecificity[3]+=RF.getSpecificity();
                            printWriterDEV.println((testID+1)+":\tWeighted: "+RF.getAccuracy()+"\t"+RF.getSensitivity()+"\t"+RF.getSpecificity());
                        RF.growForest(SplitCriterion.TWOING);
                        RF.evaluate(nTree,VoteType.MAJORITY);
                            averageAccuracy[4]+=RF.getAccuracy();
                            averageSensitivity[4]+=RF.getSensitivity();
                            averageSpecificity[4]+=RF.getSpecificity();
                            printWriterTWO.println((testID+1)+":\tMajority: "+RF.getAccuracy()+"\t"+RF.getSensitivity()+"\t"+RF.getSpecificity());
                        RF.evaluate(nTree,VoteType.WEIGHTED);
                            averageAccuracy[5]+=RF.getAccuracy();
                            averageSensitivity[5]+=RF.getSensitivity();
                            averageSpecificity[5]+=RF.getSpecificity();
                            printWriterTWO.println((testID+1)+":\tWeighted: "+RF.getAccuracy()+"\t"+RF.getSensitivity()+"\t"+RF.getSpecificity());
                    }
                    System.out.println("\r\t-Calculating forest prediction: ["+nTest+"/"+nTest+"]");
                    for(int i=0;i<6;i++){
                        averageAccuracy[i]/=nTest;
                        averageAccuracy[i]=round(averageAccuracy[i]*10000)/10000d;
                        averageSensitivity[i]/=nTest;
                        averageSensitivity[i]=round(averageSensitivity[i]*10000)/10000d;
                        averageSpecificity[i]/=nTest;
                        averageSpecificity[i]=round(averageSpecificity[i]*10000)/10000d;
                    }
                    System.out.println("\t-Saving logs");
                    printWriterGDI.println("\nAverage:\taccuracy\tsensitivity\tspecificity");
                    printWriterGDI.println("Majority:\t"+averageAccuracy[0]+"\t"+averageSensitivity[0]+"\t"+averageSpecificity[0]);
                    printWriterGDI.println("Weighted:\t"+averageAccuracy[1]+"\t"+averageSensitivity[1]+"\t"+averageSpecificity[1]);
                    printWriterDEV.println("\nAverage:\taccuracy\tsensitivity\tspecificity");
                    printWriterDEV.println("Majority:\t"+averageAccuracy[2]+"\t"+averageSensitivity[2]+"\t"+averageSpecificity[2]);
                    printWriterDEV.println("Weighted:\t"+averageAccuracy[3]+"\t"+averageSensitivity[3]+"\t"+averageSpecificity[3]);
                    printWriterTWO.println("\nAverage:\taccuracy\tsensitivity\tspecificity");
                    printWriterTWO.println("Majority:\t"+averageAccuracy[4]+"\t"+averageSensitivity[4]+"\t"+averageSpecificity[4]);
                    printWriterTWO.println("Weighted:\t"+averageAccuracy[5]+"\t"+averageSensitivity[5]+"\t"+averageSpecificity[5]);
                    printWriterGDI.close();
                    printWriterDEV.close();
                    printWriterTWO.close();
                }catch (FileNotFoundException ex) {
                    System.out.println("\nERROR: Unable to create comparison log files");
                    return;
                }
                System.out.println("Finished. Check the following directory: "+System.getProperty("user.home")+"\\Documents\\RFDatasets");
                System.out.println("to review the log files");
            }
            if(appAction==AppAction.CALCULATE_ERROR_RATES || appAction==AppAction.ALL){
                System.out.println("--==Calculating Error Rates==--");
                RandomForest RF=new RandomForest();
                RF.setMaxDepth(depthLimit);
                RF.loadFile(filename, splitBy, variablesNames, targetIndex,positiveClass,missingValues);

                double[] avgErrorRates=new double[nTree];
                for(int i=0;i<avgErrorRates.length;i++)
                    avgErrorRates[i]=0;

                for(int testID=0;testID<nTest;testID++){
                    System.out.print("\r\t-Completed test cases: "+testID+"/"+nTest);
                    RF.setSeed(System.currentTimeMillis());
                    RF.prepareValidationSamples();
                    RF.seedForest(nTree,mTry);
                    RF.growForest(SplitCriterion.GDI);
                    for(int i=0;i<nTree;i++){
                        RF.evaluate(i+1, VoteType.MAJORITY);
                        avgErrorRates[i]+=RF.getErrorRate();
                    }
                }
                System.out.println("\r\t-Completed test cases:" +nTest+"/"+nTest);
                try {
                    System.out.println("\t-Saving logs");
                    PrintWriter printWriter;
                    String filename2=filename.replace(".txt","");
                    filename2=filename.replace(".csv","");
                    printWriter=new PrintWriter(new File(Paths.get(System.getProperty("user.home"),"Documents\\RFDatasets",filename2+"-averageErrorRates.txt").toString()));
                    for(int i=0;i<avgErrorRates.length;i++){
                        avgErrorRates[i]/=nTest;
                        avgErrorRates[i]=round(avgErrorRates[i]*10000)/10000d;
                        printWriter.println((i+1)+"\t"+avgErrorRates[i]);
                    }
                    printWriter.close();
                } catch (FileNotFoundException ex) {
                    System.out.println("\nERROR: Unable to create errorRates log file");
                    return;
                }
                System.out.println("Finished. Check the following directory: "+System.getProperty("user.home")+"\\Documents\\RFDatasets");
                System.out.println("to review the log files");
            }   
        }
        catch(OutOfMemoryError e){
            System.out.println("\nERROR: Not enought memory. Chceck if given parameters were correct");
        }
        catch(Exception e){
            System.out.println("\nERROR: Unexpected behaviour. Run stopped");
        }
        // TODO code application logic here
    }
}
