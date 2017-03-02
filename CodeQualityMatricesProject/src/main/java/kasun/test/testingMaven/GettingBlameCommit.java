/*
 *  Copyright (c) Jan 31, 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */



package kasun.test.testingMaven;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class GettingBlameCommit extends CallingAPI {

    private String jsonOutPutFileOfSearchCommitAPI="jsonOutPutFileOfSearchCommitAPI.json";
    private String urlForObtainingCommits,urlForGetingFilesChanged;

    protected ArrayList<String> fileNames = new ArrayList<String>();
    protected List<ArrayList<String>> lineRangesChanged= new ArrayList<ArrayList<String>>();      // for saving the line no that are changed
    JSONObject graphqlApiJsonObject= new JSONObject();

    Set <String> commitHashesOfTheParent;

    // this can be taken as repo vice
    Set <String> authorNames= new HashSet<String>();    //as the authors are for all the commits that exists in the relevant patch

    protected Set <String> commitHashObtainedForPRReview= new HashSet<String>();  //  relevant commits in old file that need to find the PR Reviewers





    private String repoLocation[];


    public String getUrlForSearchingCommits() {
        return urlForObtainingCommits;
    }

    public void setUrlForSearchingCommits(String commitHash) {
        this.urlForObtainingCommits = "https://api.github.com/search/commits?q=hash%3A"+commitHash;
    }

    public String getUrlForGetingFilesChanged() {
        return urlForGetingFilesChanged;
    }
    public void setUrlForGetingFilesChanged(String repoName,String commitHash) {
        this.urlForGetingFilesChanged ="http://api.github.com/repos/"+repoName+"/commits/"+commitHash;
    }










    /**
     * obtaining PR for each commit and saving them in a file
     * @throws IOException
     */
    public void obtainingRepoNamesForCommitHashes() throws IOException{


        for(String commitHash: patchInformation_svnRevisionpublic){

            setUrlForSearchingCommits(commitHash);


            //calling the API calling method
            callingTheAPI(getUrlForSearchingCommits(),jsonOutPutFileOfSearchCommitAPI,true,true,false);
            saveRepoNamesInAnArray(commitHash);

        }





    }

    /**
     * saving the  Repo Names in the array and calling to Get files content
     * @param commitHash
     */
    public void saveRepoNamesInAnArray(String commitHash){
        try{


            JSONObject rootJsonObject= (JSONObject)parser.parse(new FileReader(location+jsonOutPutFileOfSearchCommitAPI));
            JSONArray jsonArrayOfItems= (JSONArray)rootJsonObject.get("items");

            // setting the size of the repoLocationArray
            repoLocation= new String [jsonArrayOfItems.size()];

            for(int i=0; i<jsonArrayOfItems.size();i++){
                JSONObject jsonObject = (JSONObject) jsonArrayOfItems.get(i);

                JSONObject repositoryJsonObject= (JSONObject)jsonObject.get("repository");

                //adding the repo name to the array
                repoLocation[i]= (String)repositoryJsonObject.get("full_name");

            }
        }

        catch(FileNotFoundException e){
            e.printStackTrace();
        }
        catch(ParseException e){
            e.printStackTrace();
        }
        catch(IOException e){
            e.printStackTrace();
        }

        //        for running through the repoName Array 
        for(int i =0; i< repoLocation.length;i++){

            if(StringUtils.contains(repoLocation[i],"wso2/")){

                //clearing all the data in the current fileNames and lineRangesChanged arraylists for each repository
                fileNames.clear();
                lineRangesChanged.clear();
                //                authorNames.clear();


                callingToGetFilesChanged(repoLocation[i],commitHash); 


                //            calling the graphql API for getting blame information


                //====================================================================================================================================================
                //                try {

                //                    callingGraphqlApi(repoLocation[i],commitHash,false);
                //
                //                    // reading the blame thus received from graphql API
                //                    readingBlameOfFile(repoLocation[i],commitHash,false);





                //                } catch (IOException e) {
                //                   // TODO Auto-generated catch block
                //                    e.printStackTrace();
                //                }

                //====================================================================================================================================================


                iteratingOver(repoLocation[i],commitHash); // this will iterate over and save the parent commit hashes of changes in each file in the current repo to the commitHashesOfTheParent SET






                //                =========================== testing new one ================================
            }
        }

        // for printing the author names.
        System.out.println(authorNames);
        System.out.println(commitHashObtainedForPRReview);





    }



    /**
     * calling Single commit API to get files changed in a commit
     * @param repoLocation
     * @param commitHash
     */
    public void callingToGetFilesChanged(String repoLocation, String commitHash){

        //        setting the URL for calling github single commit API

        setUrlForGetingFilesChanged(repoLocation,commitHash);

        //file name for saving the output
        String savingLocation= repoLocation+"/"+commitHash+".json";

        //saving the commit details for the commit hash on the relevant repository
        try {
            callingTheAPI(getUrlForGetingFilesChanged(), savingLocation, true,false,false);

        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } 


        //----calling savingRelaventFileNamesAndEditLineNumbers method to read the above saved json output----
        savingRelaventFileNamesAndEditLineNumbers(savingLocation);





    }

    /**
     * saving relevant file names and their edit line numbers
     * @param savingLocation
     */
    public void savingRelaventFileNamesAndEditLineNumbers(String savingLocation){

        //read the json output
        try {

            JSONObject rootJsonObject=(JSONObject) parser.parse(new FileReader(location+savingLocation));
            Object checkObject= rootJsonObject.get("files");
            //checking if the json stream received for element "files" is a jsonObject or a jsonArray

            if(checkObject instanceof JSONObject ){
                // if it is a JSONObject then only one file has been changed from the commit

                // then casting the checkObject to a JSONObject
                JSONObject filesJsonObject=(JSONObject)checkObject;
                String fileName=(String)filesJsonObject.get("filename");
                fileNames.add(fileName);

                //filtering only the line no that are modified

                String patch= (String)filesJsonObject.get("patch");
                String lineChanges[]=StringUtils.substringsBetween(patch,"@@", "@@");


            }

            // in genaral the code comes here--------------------------------------------
            else if (checkObject instanceof JSONArray){
                //                 more than one file has been changed by the relevant commit
                JSONArray fileJsonArray= (JSONArray)checkObject;

                // to save one file at a time
                for(int i =0; i< fileJsonArray.size();i++){
                    JSONObject tempJsonObject= (JSONObject) fileJsonArray.get(i);
                    String fileName= (String)tempJsonObject.get("filename");
                    //saving the file name in the filename arraylist
                    fileNames.add(fileName);

                    //filtering only the line ranges that are modified and saving to a string array
                    String patch= (String)tempJsonObject.get("patch");
                    String lineChanges[]= StringUtils.substringsBetween(patch,"@@ "," @@");

                    //filtering the lines that existed in the previous file, that exists in the new file and saving them in to the same array
                    for (int j=0; j<lineChanges.length;j++){

                        //@@ -22,7 +22,7 @@
                        String tempString= lineChanges[j];
                        String lineRangeInTheOldFileBeingModified = StringUtils.substringBetween(tempString,"-"," +");      // for taking the authors and commit hashes of the previous lines
                        String lineRangeInTheNewFileResultedFromModification= StringUtils.substringAfter(tempString, "+");  // for taking the parent commit

                        int intialLineNoInOldFile= Integer.parseInt(StringUtils.substringBefore(lineRangeInTheOldFileBeingModified, ","));
                        int tempEndLineNoInOldFile= Integer.parseInt(StringUtils.substringAfter(lineRangeInTheOldFileBeingModified,","));
                        int endLineNoOfOldFile;
                        if(intialLineNoInOldFile!=0){
                            // to filterout the newly created files
                            endLineNoOfOldFile= intialLineNoInOldFile+ (tempEndLineNoInOldFile-1);
                        }
                        else{
                            endLineNoOfOldFile=tempEndLineNoInOldFile;
                        }

                        int intialLineNoInNewFile= Integer.parseInt(StringUtils.substringBefore(lineRangeInTheNewFileResultedFromModification, ","));
                        int tempEndLineNoInNewFile= Integer.parseInt(StringUtils.substringAfter(lineRangeInTheNewFileResultedFromModification,","));
                        int endLineNoOfNewFile= intialLineNoInNewFile+ (tempEndLineNoInNewFile-1);



                        // storing the line ranges that are being modified in the same array by replacing values
                        lineChanges[j]=intialLineNoInOldFile+","+endLineNoOfOldFile+"/"+intialLineNoInNewFile+","+endLineNoOfNewFile;

                    }

                    ArrayList<String> tempArrayList= new ArrayList<String>(Arrays.asList(lineChanges));

                    //adding to the array list which keep track of the line ranges which are being changed to the main arrayList
                    lineRangesChanged.add(tempArrayList);







                }

                System.out.println("done saving file names and their relevant modification line ranges");
                System.out.println(fileNames);
                System.out.println(lineRangesChanged);


            }






        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (ParseException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }





    }

    /**
     * iterating and calling graphql
     * @param repoLocation
     * @param commitHash
     */
    public void iteratingOver(String repoLocation, String commitHash){

        // filtering the owner and the repository name from the repoLocation
        String owner= StringUtils.substringBefore(repoLocation,"/");
        String repositoryName= StringUtils.substringAfter(repoLocation,"/");

        String locationOfTheSavedFile= null;


        //        iterating over the fileNames arraylist for the given commit
        Iterator iteratorForFileNames= fileNames.iterator();



        while(iteratorForFileNames.hasNext()){
            String fileName= (String)iteratorForFileNames.next();
            int index= fileNames.indexOf(fileName);
            // the relevant arraylist of changed lines for that file

            ArrayList <String> arrayListOfRelevantChangedLines=lineRangesChanged.get(index);


            commitHashesOfTheParent= new HashSet<String>();   // for storing the parent commit hashes for all the line ranges of the relevant file

            graphqlApiJsonObject.put("query","{repository(owner:\""+owner+"\",name:\""+repositoryName+"\"){object(expression:\""+commitHash+"\"){ ... on Commit{blame(path:\""+fileName+"\"){ranges{startingLine endingLine age commit{history(first: 2) { edges { node {  message url } } } author { name email } } } } } } } }");

            try{
                //            calling the graphql API for getting blame information for the current file and saving it in a location.
                locationOfTheSavedFile= callingGraphQl(graphqlApiJsonObject,fileName,commitHash,repoLocation);

            }
            catch(IOException e){
                e.printStackTrace();
            }
            //            reading the above saved output for the current file name





            readingTheBlameReceivedForAFileName(locationOfTheSavedFile,fileName,owner,repositoryName,repoLocation,arrayListOfRelevantChangedLines,false);

            // parent commit hashes are stored in the arraylist for the given file

            iteratingOverForFindingAuthors(owner,repositoryName,fileName,repoLocation,arrayListOfRelevantChangedLines);





        }


    }
    
    
    /**
     * Calling the github graphQL API
     * @param queryObject
     * @param fileName
     * @param commitHash
     * @param repoLocation
     * @return
     * @throws IOException
     */
    public String callingGraphQl(JSONObject queryObject,String fileName,String commitHash,String repoLocation) throws IOException{

        CloseableHttpClient client= null;
        CloseableHttpResponse response= null;


        client= HttpClients.createDefault();
        HttpPost httpPost= new HttpPost("https://api.github.com/graphql");

        httpPost.addHeader("Authorization","Bearer "+getToken());
        httpPost.addHeader("Accept","application/json");

        try {

            //                StringEntity entity= new StringEntity("{\"query\":\"query "+graphqlQuery+"\"}");
            StringEntity entity= new StringEntity(queryObject.toString());


            httpPost.setEntity(entity);
            response= client.execute(httpPost);

        }

        catch(UnsupportedEncodingException e){
            e.printStackTrace();
        }
        catch(ClientProtocolException e){
            e.printStackTrace();
        }
        catch(IOException e){
            e.printStackTrace();
        }


        BufferedWriter bufferedWritter=null;
        BufferedReader bufferedReader=null;
        String saveLocation=null;
        try{
            bufferedReader= new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
            String line= null;
            StringBuilder stringBuilder= new StringBuilder();
            while((line=bufferedReader.readLine())!= null){

                stringBuilder.append(line);

            }

            //renaming the file .java to _java as it may conflict with the .json extension (if not will result in 2 extensions for the same file)
            String fileNameWithoutExtension= StringUtils.substringBeforeLast(fileName, ".");
            String fileNamesExtension= StringUtils.substringAfterLast(fileName,".");

            String modifiedFileName= fileNameWithoutExtension+"_"+fileNamesExtension;


            // saving the output received to a file located in the same repo directory under the same owner of the repo
            saveLocation= repoLocation+"/FileChanged/"+commitHash+"/"+modifiedFileName+"_blame.json";


            File fileLocation= new File(location+saveLocation);
            fileLocation.getParentFile().mkdirs();      //creating directories according to the file name given
            bufferedWritter= new BufferedWriter(new FileWriter(fileLocation));

            bufferedWritter.write(stringBuilder.toString());

            //            System.out.println(stringBuilder.toString());
        }
        catch(Exception e){
            e.printStackTrace();
        }

        finally{
            if(bufferedWritter !=null){
                bufferedWritter.close();

            }
            if(bufferedReader != null){
                bufferedReader.close();}
        }

        return saveLocation;



    }

    //    
    //    public void readingTheBlameReceivedForAFileName(String locationOfTheSavedFile,String fileName,String owner,String repositoryName,String repoLocation,ArrayList<String> arrayListOfRelevantChangedLines){
    //    
    //        
    //     // using an Iterator for iterating in the lineRangesChanged arrayList 
    //        Iterator lineNoIterator = arrayListOfRelevantChangedLines.iterator();
    //
    //        while(lineNoIterator.hasNext()){
    //
    //            // this gives an arraylist of the line ranges for the file exists in the current index of the fileName array list
    //
    //            ArrayList<String> lineRangesOfAffectedFile= (ArrayList<String>)lineNoIterator.next();
    //            
    //            
    //            readingTheBlameReceivedForAFileName2(locationOfTheSavedFile, fileName, owner, repositoryName, repoLocation,lineRangesOfAffectedFile);
    //            
    //            
    //        }
    //        
    //      
    //    
    //    
    //    } 

    
    /**
     * Reading the blame received for a given file name
     * @param locationOfTheSavedFile
     * @param fileName
     * @param owner
     * @param repositoryName
     * @param repoLocation
     * @param arrayListOfRelevantChangedLines
     * @param gettingPr
     */
    public void readingTheBlameReceivedForAFileName(String locationOfTheSavedFile,String fileName,String owner,String repositoryName,String repoLocation,ArrayList<String> arrayListOfRelevantChangedLines,boolean gettingPr){




        //running a iterator for fileName arrayList to get the location of the above saved file

        try{
            JSONObject rootJSONObject =(JSONObject) parser.parse(new FileReader(location+locationOfTheSavedFile));
            JSONObject dataJSONObject= (JSONObject)rootJSONObject.get("data");  
            JSONObject repositoryJSONObect= (JSONObject) dataJSONObject.get("repository");
            JSONObject objectJSONObject=(JSONObject)repositoryJSONObect.get("object");
            JSONObject blameJSONObject= (JSONObject) objectJSONObject.get("blame");
            JSONArray rangeJSONArray= (JSONArray) blameJSONObject.get("ranges");

            // --------------------getting the starting line no of the range of lines that are modified from the patch-----------------



            Iterator arrayListOfRelevantChangedLinesIterator= arrayListOfRelevantChangedLines.iterator();     // iterator for the array list inside the root arraylist


            while (arrayListOfRelevantChangedLinesIterator.hasNext()){

                int startingLineNo;
                int endLineNo;

                String lineRanges= (String)arrayListOfRelevantChangedLinesIterator.next();

                String oldFileRange=StringUtils.substringBefore(lineRanges,"/");
                String newFileRange= StringUtils.substringAfter(lineRanges,"/");

                // need to skip the newly created files from taking the blame
                if(oldFileRange.equals("0,0")){

                    continue;   

                }
                //new change
                else{

                    if(gettingPr==true){
                        // need to consider the line range in the old file for finding authors and reviewers

                        startingLineNo=Integer.parseInt(StringUtils.substringBefore(oldFileRange,","));
                        endLineNo=Integer.parseInt(StringUtils.substringAfter(oldFileRange,","));



                    }

                    else{
                        // need to consider the line range in the new file resulted from applying the commit for finding parent commits

                        startingLineNo= Integer.parseInt(StringUtils.substringBefore(newFileRange,","));
                        endLineNo= Integer.parseInt(StringUtils.substringAfter(newFileRange,","));
                    }



                    // as it is required to create a new Map for finding the recent commit for each line range 
                    Map <Long,ArrayList<Integer>> mapForStoringAgeAndIndex= new HashMap<Long, ArrayList<Integer>> ();




                    //checking line by line by iterating the startinLineNo

                    while(endLineNo>=startingLineNo){
                        //running through the rangeJSONArray

                        for(int i=0; i<rangeJSONArray.size();i++){

                            JSONObject rangeJSONObject= (JSONObject) rangeJSONArray.get(i);

                            long tempStartingLineNo= (Long)rangeJSONObject.get("startingLine");
                            long tempEndingLineNo=(Long)rangeJSONObject.get("endingLine");




                            //checking whether the line belongs to that line range group
                            if((tempStartingLineNo<=startingLineNo) &&(tempEndingLineNo>=startingLineNo)){
                                // so the relevant startingLineNo belongs in this line range in other words in this JSONObject


                                if(gettingPr==false){
                                    long age =(Long)rangeJSONObject.get("age");



                                    // storing the age field with relevant index of the JSONObject
                                    mapForStoringAgeAndIndex.putIfAbsent(age, new ArrayList<Integer>());
                                    if(!mapForStoringAgeAndIndex.get(age).contains(i)){
                                        mapForStoringAgeAndIndex.get(age).add(i);   // adding if the index is not present in the array list for the relevant age
                                    }

                                }
                                else{
                                    //for saving the author names of commiters
                                    JSONObject commitJSONObject= (JSONObject) rangeJSONObject.get("commit");

                                    JSONObject authorJSONObject= (JSONObject) commitJSONObject.get("author");
                                    String nameOfTheAuthor= (String) authorJSONObject.get("name");
                                    authorNames.add(nameOfTheAuthor);       // authors are added to the Set

                                    String urlOfCommit= (String)commitJSONObject.get("url");
                                    String commitHashForPRReview= StringUtils.substringAfter(urlOfCommit,"commit/");
                                    commitHashObtainedForPRReview.add(commitHashForPRReview);









                                }





                                break;

                            }
                            else{
                                continue;
                            }





                        }



                        startingLineNo++;   // to check for other line numbers



                    }

                    //---------------------------for the above line range getting the lastest commit which modified the lines--------------------------

                    if(gettingPr==false){
                        //converting the map into a treeMap to get it ordered

                        TreeMap <Long, ArrayList<Integer>> treeMap= new TreeMap<>(mapForStoringAgeAndIndex);
                        Long minimumKeyOfMapForStoringAgeAndIndex= treeMap.firstKey(); // getting the minimum key

                        //                     getting the relevant JSONObject indexes which consists of the recent commit with in the relevant line range
                        ArrayList<Integer> indexesOfJsonObjectForRecentCommit= mapForStoringAgeAndIndex.get(minimumKeyOfMapForStoringAgeAndIndex);

                        Iterator indexesOfJsonObjectForRecentCommitIterator = indexesOfJsonObjectForRecentCommit.iterator();

                        while (indexesOfJsonObjectForRecentCommitIterator.hasNext()){
                            int index= (int)indexesOfJsonObjectForRecentCommitIterator.next();


                            // this is the range where the code gets actually modified

                            JSONObject rangeJSONObject= (JSONObject) rangeJSONArray.get(index);
                            JSONObject commitJSONObject= (JSONObject) rangeJSONObject.get("commit");
                            JSONObject historyJSONObject= (JSONObject) commitJSONObject.get("history");
                            JSONArray edgesJSONArray =(JSONArray) historyJSONObject.get("edges");

                            //getting the second json object from the array as it contain the commit of the parent which modified the above line range
                            JSONObject edgeJSONObject= (JSONObject) edgesJSONArray.get(1);

                            JSONObject nodeJSONObject=(JSONObject) edgeJSONObject.get("node");


                            String urlOfTheParentCommit= (String) nodeJSONObject.get("url");       // this contain the URL of the parent commit

                            String commitHash=(String)StringUtils.substringAfter(urlOfTheParentCommit, "commit/");


                            commitHashesOfTheParent.add(commitHash);





                        }

                    }

                    else{



                    }

                    // parent commits for whole ranges of the current file are added to the commitHashesOfTheParent Set













                    //                    // the programms starts to check for the other line range of the same file, if a next range exists (git always keep a new line range for each modification)


                }
            }







        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (ParseException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    
    /**
     * Finding the authors of the commits
     * @param owner
     * @param repositoryName
     * @param fileName
     * @param repoLocation
     * @param arrayListOfRelevantChangedLines
     */
    public void iteratingOverForFindingAuthors(String owner,String repositoryName,String fileName,String repoLocation,ArrayList<String> arrayListOfRelevantChangedLines){


        // calling the graphql api to get the blame details of the current file for the parent commits (That is found by filtering in the graqhql output)

        Iterator commitHashOfTheParentIterator= commitHashesOfTheParent.iterator();

        while(commitHashOfTheParentIterator.hasNext()){
            String parentCommitHashForCallingGraphQl= (String)commitHashOfTheParentIterator.next();


            graphqlApiJsonObject.put("query", "{repository(owner:\""+owner+"\",name:\""+repositoryName+"\"){object(expression:\""+parentCommitHashForCallingGraphQl+"\"){ ... on Commit{blame(path:\""+fileName+"\"){ranges{startingLine endingLine age commit{ url author { name email } } } } } } } }");


            try{

                String locationOfTheSavedFile= callingGraphQl(graphqlApiJsonObject,fileName, parentCommitHashForCallingGraphQl,repoLocation);
                readingTheBlameReceivedForAFileName(locationOfTheSavedFile, fileName, owner, repositoryName, repoLocation, arrayListOfRelevantChangedLines,true);


            }
            catch(IOException e){
                e.printStackTrace();
            }


        }

    }















    //  =========================================================================================================================================================  























    //============================= calling the graphql of GitHub through apache httpClient, this is only for a single repository ==========================================

    public void callingGraphqlApi(String repoLocation, String commitHash,boolean callingToGetBlameForSecondTime) throws IOException{

        // filtering the owner and the repository name from the repoLocation
        String owner= StringUtils.substringBefore(repoLocation,"/");
        String repositoryName= StringUtils.substringAfter(repoLocation,"/");



        CloseableHttpClient client= null;
        CloseableHttpResponse response= null;

        client= HttpClients.createDefault();
        HttpPost httpPost= new HttpPost("https://api.github.com/graphql");

        httpPost.addHeader("Authorization","Bearer "+getToken());
        httpPost.addHeader("Accept","application/json");


        //      using an iterator for looping through the arraylists of filenames
        Iterator iteratorForFileNames = fileNames.iterator();

        //saving the blame of each file that changed in the relevant commit hash
        while (iteratorForFileNames.hasNext()){
            String fileName= (String)iteratorForFileNames.next();



            //        String temp="{viewer {email login }}";
            //        String temp="{repository(owner:\\\"wso2\\\",name:\\\"product-is\\\"){description}}";

            //            String graphqlQuery=null;
            JSONObject graphqlApiJsonObject= null;

            if(callingToGetBlameForSecondTime == false){

                //query for calling the graphql for getting the blame of the merge commit that resides in PMT API output
                //                graphqlQuery="{repository(owner:\\\""+owner+"\\\",name:\\\""+repositoryName+"\\\"){object(expression:\\\""+commitHash+"\\\"){ ... on Commit{blame(path:\\\""+fileName+"\\\"){ranges{startingLine endingLine age commit{history(first: 2) { edges { node {  message url } } } author { name email } } } } } } } }";

                graphqlApiJsonObject= new JSONObject();
                graphqlApiJsonObject.put("query","{repository(owner:\""+owner+"\",name:\""+repositoryName+"\"){object(expression:\""+commitHash+"\"){ ... on Commit{blame(path:\""+fileName+"\"){ranges{startingLine endingLine age commit{history(first: 2) { edges { node {  message url } } } author { name email } } } } } } } }");
            }
            else{

                //query for calling the graphql for obtaining the blame details of the parent commit that actually changed those line ranges
                //                graphqlQuery="{repository(owner:\\\""+owner+"\\\",name:\\\""+repositoryName+"\\\"){object(expression:\\\""+commitHash+"\\\"){ ... on Commit{blame(path:\\\""+fileName+"\\\"){ranges{startingLine endingLine age commit{ url author { name email } } } } } } } }";

                graphqlApiJsonObject = new JSONObject();
                graphqlApiJsonObject.put("query", "{repository(owner:\""+owner+"\",name:\""+repositoryName+"\"){object(expression:\""+commitHash+"\"){ ... on Commit{blame(path:\""+fileName+"\"){ranges{startingLine endingLine age commit{ url author { name email } } } } } } } }");
            }



            try {

                //                StringEntity entity= new StringEntity("{\"query\":\"query "+graphqlQuery+"\"}");
                StringEntity entity= new StringEntity(graphqlApiJsonObject.toString());


                httpPost.setEntity(entity);
                response= client.execute(httpPost);

            }

            catch(UnsupportedEncodingException e){
                e.printStackTrace();
            }
            catch(ClientProtocolException e){
                e.printStackTrace();
            }
            catch(IOException e){
                e.printStackTrace();
            }


            BufferedWriter bufferedWritter=null;
            BufferedReader bufferedReader=null;
            try{
                bufferedReader= new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
                String line= null;
                StringBuilder stringBuilder= new StringBuilder();
                while((line=bufferedReader.readLine())!= null){

                    stringBuilder.append(line);

                }

                //renaming the file .java to _java as it may conflict with the .json extension (if not will result in 2 extensions for the same file)
                String fileNameWithoutExtension= StringUtils.substringBefore(fileName, ".");
                String fileNamesExtension= StringUtils.substringAfter(fileName,".");

                String modifiedFileName= fileNameWithoutExtension+"_"+fileNamesExtension;


                // saving the output received to a file located in the same repo directory under the same owner of the repo
                String saveLocation= repoLocation+"/FileChanged/"+commitHash+"/"+modifiedFileName+"_blame.json";

                File fileLocation= new File(location+saveLocation);
                fileLocation.getParentFile().mkdirs();      //creating directories according to the file name given
                bufferedWritter= new BufferedWriter(new FileWriter(fileLocation));
                bufferedWritter.write(stringBuilder.toString());

                System.out.println(stringBuilder.toString());
            }
            catch(Exception e){
                e.printStackTrace();
            }

            finally{
                if(bufferedWritter !=null){
                    bufferedWritter.close();

                }
                if(bufferedReader != null){
                    bufferedReader.close();}
            }


        }




    }


    //    ======================= reading blame of each file thus changed from applying the patch=======================

    public void readingBlameOfFile(String repoLocation, String commitHash,boolean toCollectCommitHashesForFindingPrs){



        boolean foundTheActualCommitRange= false;
        //running a iterator for fileName arrayList to get the location of the above saved file

        //--- here we cannot call this method after running the code in callingGraphqlApi as we call the same method in here-----------------
        Iterator fileNameIterator = fileNames.iterator();

        while (fileNameIterator.hasNext()){
            String fileName=(String)fileNameIterator.next();


            //renaming the file .java to _java as it may conflict with the .json extension (if not will result in 2 extensions for the same file)
            String fileNameWithoutExtension= StringUtils.substringBefore(fileName, ".");
            String fileNamesExtension= StringUtils.substringAfter(fileName,".");

            String modifiedFileName= fileNameWithoutExtension+"_"+fileNamesExtension;


            // saving the output received to a file located in the same repo directory under the same owner of the repo
            String locationOfTheSavedFile= repoLocation+"/FileChanged/"+commitHash+"/"+modifiedFileName+"_blame.json";


            //reading the JSON received from the graphql API
            try {



                JSONObject rootJSONObject =(JSONObject) parser.parse(new FileReader(location+locationOfTheSavedFile));
                JSONObject dataJSONObject= (JSONObject)rootJSONObject.get("data");  
                JSONObject repositoryJSONObect= (JSONObject) dataJSONObject.get("repository");
                JSONObject objectJSONObject=(JSONObject)repositoryJSONObect.get("object");
                JSONObject blameJSONObject= (JSONObject) objectJSONObject.get("blame");
                JSONArray rangeJSONArray= (JSONArray) blameJSONObject.get("ranges");

                // getting the starting line no of the range of lines that are modified from the patch

                // using an Iterator for iterating in the lineRangesChanged arrayList 
                Iterator lineNoIterator = lineRangesChanged.iterator();
                while(lineNoIterator.hasNext()){

                    // this gives an arraylist of the line ranges for the file exists in the current index of the fileName array list

                    ArrayList<String> lineRangesOfAffectedFile= (ArrayList<String>)lineNoIterator.next();

                    Iterator lineRangesOfAffectedFileIterator= lineRangesOfAffectedFile.iterator();

                    while (lineRangesOfAffectedFileIterator.hasNext()){

                        String lineRanges= (String)lineRangesOfAffectedFileIterator.next();
                        int startingLineNo= Integer.parseInt(StringUtils.substringBefore(lineRanges,","));
                        int endLineNo= Integer.parseInt(StringUtils.substringAfter(lineRanges,","));



                        //checking line by line by iterating the startinLineNo

                        while(endLineNo>=startingLineNo){
                            //running through the rangeJSONArray

                            for(int i=0; i<rangeJSONArray.size();i++){

                                JSONObject rangeJSONObject= (JSONObject) rangeJSONArray.get(i);

                                long tempStartingLineNo= (Long)rangeJSONObject.get("startingLine");
                                long tempEndingLineNo=(Long)rangeJSONObject.get("endingLine");

                                //checking whether the line belongs to that line range group
                                if((tempStartingLineNo<=startingLineNo) &&(tempEndingLineNo>=startingLineNo)){
                                    // so the relevant startingLineNo belongs in this line range in other words in this JSONObject


                                    // ===================================== think here on a solution to reuse this code for obtainig the url of PRs. use the  toCollectCommitHashesForFindingPrs===========
                                    long age =(Long)rangeJSONObject.get("age");

                                    if(age==1){

                                        // this is the range where the code gets actually modified
                                        JSONObject commitJSONObject= (JSONObject) rangeJSONObject.get("commit");
                                        JSONObject historyJSONObject= (JSONObject) commitJSONObject.get("history");
                                        JSONArray edgesJSONArray =(JSONArray) historyJSONObject.get("edges");


                                        //getting the second json object from the array as it contain the commit of the parent which modified the above line range
                                        JSONObject edgeJSONObject= (JSONObject) edgesJSONArray.get(1);

                                        JSONObject nodeJSONObject=(JSONObject) edgeJSONObject.get("node");


                                        String urlOfTheParentCommit= (String) nodeJSONObject.get("url");       // this contain the URL of the parent commit

                                        //obtaining the repo name from the repository
                                        String repoName =StringUtils.substringBetween(urlOfTheParentCommit, "github.com/", "/commit");
                                        String commitHashOfTheParent= (String)StringUtils.substringAfter(urlOfTheParentCommit, "commit/");

                                        //calling the graphql api to get the blame details for the parent commit
                                        callingGraphqlApi(repoName, commitHashOfTheParent, true);

                                        //setting  foundTheActualCommitRange true as we have found the line range that contain the given line of code
                                        foundTheActualCommitRange=true;
                                        break;







                                    }
                                    else{
                                        continue;
                                    }





                                }
                                else{
                                    continue;
                                }





                            }


                            if(foundTheActualCommitRange== true){
                                break;      // to avoid looping through line numbers as we have found the correct line range

                            }

                            else{
                                startingLineNo++;
                            }


                        }




                        // the programms starts to check for the other line range of the same file, if a next range exists (git always keep a new line range for each modification)



                    }



                }


            } catch (FileNotFoundException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (ParseException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }





        }





    }

    //============================= calling the graphql of GitHub through running curl command ==========================================

    //        public void callingGraphqlApi(String repoLocation,String commitHash){
    //    
    //            String owner= StringUtils.substringBefore(repoLocation,"/");
    //            String repositoryName= StringUtils.substringAfter(repoLocation,"/");
    //    
    //    
    //    
    //    
    //            // url for graphql API
    //            String apiUrl= "https://api.github.com/graphql";
    //    
    //    
    //            //        using an iterator for looping through the arraylists of filenames
    //    
    //    
    //            Iterator iteratorForFileNames= fileNames.iterator();
    //            while(iteratorForFileNames.hasNext()){
    //    
    //                String fileName= (String)iteratorForFileNames.next();
    //    
    //    
    //                String[] curlCommand = {"curl", "-H" ,"Authorization: Bearer "+getToken(),"-H","Accept:application/json","-X", "POST", "-d", "{\"query\": \"query { repository(owner: \\\""+owner+"\\\", name: \\\""+repositoryName+"\\\") { object(expression:\\\""+commitHash+"\\\") { ... on Commit { blame(path: \\\""+fileName+"\\\") { ranges { startingLine endingLine age commit { history(first: 2) { edges { node { message url } } } author { name email } } } } } } } }\"}" , apiUrl};
    //                
    //    //            creating a processBuilder instance for running the curl command with the given set of arguments
    //                ProcessBuilder processBuilder= new ProcessBuilder(curlCommand);
    //                Process p;
    //                
    //                
    //                try{
    //                    p= processBuilder.start(); // staring new subprocess with uses the default working directory and environment variables
    //                    BufferedReader bufferedReader= new BufferedReader(new InputStreamReader(p.getInputStream()));   //obtaining the output of the subprocess through the Parent Process inputStream
    //                    StringBuilder stringBuilder= new StringBuilder();
    //                    String line=null;
    //                    while((line=bufferedReader.readLine())!= null){
    //                        stringBuilder.append(line);
    //                        stringBuilder.append(System.getProperty("line.separator"));
    //                        
    //                        
    //                        
    //                    }
    //                    System.out.println(stringBuilder.toString());
    //                    
    //                }
    //                catch(IOException e){
    //                    e.printStackTrace();
    //                }
    //                
    //                
    //                
    //               
    //                
    //               
    //                
    //                
    //    
    //    
    //    
    //            }
    //    
    //    //        String[] command = {"curl", "-H" ,"Authorization: Bearer"+getToken(),"-H","Accept:application/json","-X", "POST", "-d", "{\"query\": \"query { repository(owner: \\\"wso2-extensions\\\", name: \\\"identity-inbound-auth-oauth\\\") { object(expression:\\\"83253ce50f189db30c54f13afa5d99021e2d7ece\\\") { ... on Commit { blame(path: \\\"components/org.wso2.carbon.identity.oauth.endpoint/src/main/java/org/wso2/carbon/identity/oauth/endpoint/authz/OAuth2AuthzEndpoint.java\\\") { ranges { startingLine endingLine age commit { message url history(first: 2) { edges { node { message url } } } author { name email } } } } } } } }\"}" , apiUrl};
    //    
    //        }


















    //        public void callGraphqlApi(String repoLocation,String commitHash){
    //        
    //            CloseableHttpClient httpClientForGraphql = null;
    //            CloseableHttpResponse httpResponseFromGraphql= null;
    //    
    //    
    //            // same token for calling github API are used for the calling the github Graphql API
    //    
    //            httpClientForGraphql=HttpClients.createDefault();
    //            HttpPost httpPost= new HttpPost("https://api.github.com/graphql");
    //    
    //            //       =============== work in here=============================
    //    
    //    
    //            JSONObject jsonObject=  new JSONObject();
    //            JSONObject p = new JSONObject();
    //            p.put("login", null);
    //    
    //            jsonObject.put("viewer", p);
    //    
    //    
    //    
    //            //        String query="{\"query\": \"query {repository(owner:\"wso2\",name:\"product-is\"){description}}\"}";
    //    
    //            //        String query= "{\"query\":\"query { repository(owner: \"wso2-extensions\", name:\"identity-inbound-auth-oauth\") { object(expression: \"83253ce50f189db30c54f13afa5d99021e2d7ece\") { ... on Commit { blame(path: \"components/org.wso2.carbon.identity.oauth.endpoint/src/main/java/org/wso2/carbon/identity/oauth/endpoint/authz/OAuth2AuthzEndpoint.java\") { ranges { startingLine endingLine age commit { message url history(first: 2) { edges { node { message url } } } author { name email } } } } } } } }\"}";
    //    
    //    
    //            // passing the access token
    //            h
    //    //        httpPost.addHeader("content-type","application/x-www-form-urlencoded");
    //            httpPost.addHeader("Accept","application/json");
    //    
    //    
    //    
    //            try {
    //    
    //                //            StringEntity params= new StringEntity(query);
    //    
    //    
    //                StringEntity params= new StringEntity(jsonObject.toString());
    //    
    //                httpPost.setEntity(params);
    //                httpResponseFromGraphql= httpClientForGraphql.execute(httpPost);
    //    
    //    
    //                System.out.println("break point before");   
    //    
    //    
    //            } catch (UnsupportedEncodingException e) {
    //                // TODO Auto-generated catch block
    //                e.printStackTrace();
    //            }
    //    
    //    
    //            catch (ClientProtocolException e) {
    //                // TODO Auto-generated catch block
    //                e.printStackTrace();
    //            } catch (IOException e) {
    //                // TODO Auto-generated catch block
    //                e.printStackTrace();
    //            }
    //    
    //            System.out.println("break point before reading the content of the response");
    //    
    //            try{
    //                BufferedReader bufferedReader= new BufferedReader(new InputStreamReader(httpResponseFromGraphql.getEntity().getContent()));
    //    
    //                String line;
    //                StringBuilder stringBuilder= new StringBuilder();
    //    
    //                while((line=bufferedReader.readLine())!= null){
    //                    stringBuilder.append(line);
    //    
    //    
    //    
    //                }
    //    
    //                System.out.println(stringBuilder.toString());
    //            }
    //    
    //            catch(IOException e){
    //                e.printStackTrace();
    //            }
    //    
    //    
    //            System.out.println("Ended ");
    //    
    //    
    //    
    //    
    //    
    //    
    //        }




}


