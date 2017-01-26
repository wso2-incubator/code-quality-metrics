/*
 *  Copyright (c) Jan 17, 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Scanner;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.apache.commons.lang3.*;

public class GettingTheCommitIDFromPatchID {

    private String token;
    private String patchId;
    private String urlForObtainingCommitHashes,urlForObtainingPRs;

    protected final String location=System.getProperty("user.dir")+"/";           // to save the json output of the API call
    private String jsonOutPutFileOfCommits= "jsonOutPutFileCommits.json";
    private String jsonOutPutFileOfPRs= "jsonOutPutFilePRs.json";
    private String prHtmlUrlDetails;
    protected JSONParser parser= new JSONParser();
    private String patchInformation_svnRevisionpublic[];        // for saving the commit id of the patch

    protected ArrayList <String> productID= new ArrayList<String>(); 
    protected ArrayList <Long> prNumber = new ArrayList <Long> ();



    Scanner user_input= new Scanner(System.in);




    public String getToken() {
        return token;
    }

    public void setToken(String tokenFor) {
        System.out.println("Enter the token for "+ tokenFor);
        
        this.token= user_input.next();
       
    }
    public String getPatchId() {
        return patchId;
    }
    public void setPatchId(String patchId) {
        this.patchId = patchId;
    }
    public String getURL() {
        return urlForObtainingCommitHashes;
    }
    public void setURL(String uRL) {
        urlForObtainingCommitHashes = uRL;
    }

    public String getUrlForObtainingPRs() {
        return urlForObtainingPRs;
    }
    public void setUrlForObtainingPRs(String commitHash) {
        this.urlForObtainingPRs = "https://api.github.com/search/issues?q="+commitHash;
    }


    // =============== for setting the internal PMT API URL ============================================================
    public void setData() throws IOException{

        System.out.println("Enter the patch id");

        setPatchId(user_input.next());

        setURL("http://umt.private.wso2.com:9765/codequalitymatricesapi/1.0.0//properties?path=/_system/governance/patchs/"+getPatchId());

        callingTheAPI(urlForObtainingCommitHashes,jsonOutPutFileOfCommits,true,false);

    }


    //=========== calling the relevant API and saving the output to a file===============================================

    public void  callingTheAPI(String URL, String file,boolean requireToken,boolean requireReview) throws IOException{

        BufferedReader bufferedReader= null;
        CloseableHttpClient httpclient= null;
        CloseableHttpResponse httpResponse= null;
        BufferedWriter bufferedWriter= null;


        //================ To do: 
        //                try(BufferedReader bufferedReader= new BufferedReader(new InputStreamReader (httpResponse.getEntity().getContent()))){
        //                    StringBuilder stringBuilder= new StringBuilder();
        //                    String line;
        //                    while((line=bufferedReader.readLine())!=null){
        //                        stringBuilder.append(line);
        //        
        //                    }
        //        
        //                    System.out.println(stringBuilder.toString()); 
        //        
        //        
        //                }


        try {
            httpclient = HttpClients.createDefault();
            HttpGet httpGet= new HttpGet(URL);

            if(requireToken==true){

                httpGet.addHeader("Authorization","Bearer "+getToken());        // passing the token for the API call
            }

            //as the accept header is needed for the review API since it is still in preview mode   
            if(requireReview==true){
                httpGet.addHeader("Accept","application/vnd.github.black-cat-preview+json");

            }

            httpResponse=httpclient.execute(httpGet);
            int responseCode= httpResponse.getStatusLine().getStatusCode();     // to get the response code

            //System.out.println("Response Code: "+responseCode);

            bufferedReader= new BufferedReader(new InputStreamReader (httpResponse.getEntity().getContent()));

            StringBuilder stringBuilder= new StringBuilder();
            String line;
            while((line=bufferedReader.readLine())!=null){
                stringBuilder.append(line);

            }

            //System.out.println("Recieved JSON "+stringBuilder.toString());

            //------- writing the content received from the response to the given file
            bufferedWriter= new BufferedWriter(new FileWriter (location+file));
            bufferedWriter.write(stringBuilder.toString());
        } 

        catch (ClientProtocolException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } 
        catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        finally{
            if(bufferedWriter != null){
                bufferedWriter.close();
            }

            if (bufferedReader != null){
                bufferedReader.close();
            }

            if(httpResponse != null){
                httpResponse.close();
            }
            if (httpclient != null){
                httpclient.close();
            }


        }
    }

    //  ===================== getting the commit IDs from the above saved file ============================================

    public void getThePublicGitCommitId(){
        try{
            JSONArray jsonArray= (JSONArray)parser.parse(new FileReader(location+jsonOutPutFileOfCommits));

            for(int i=0; i<jsonArray.size();i++){
                JSONObject jsonObject = (JSONObject) jsonArray.get(i);

                String tempName= (String)jsonObject.get("name");

                if(tempName.equals("patchInformation_svnRevisionpublic")){
                    JSONArray tempCommitsJSONArray= (JSONArray)jsonObject.get("value");

                  //initializing the patchInformation_svnRevisionpublic array

                    patchInformation_svnRevisionpublic= new String [tempCommitsJSONArray.size()];  

                    for(int j =0; j< tempCommitsJSONArray.size();j++){
                        

                        patchInformation_svnRevisionpublic[j]=(String)tempCommitsJSONArray.get(j);


                    }

                    break;
                }

            }

            System.out.println("The commit Ids are");


            //            for printing all the commits ID associated with a patch
            for (String tmp: patchInformation_svnRevisionpublic){
                System.out.println(tmp);
            }
            System.out.println();

        }
        catch(FileNotFoundException e){
            System.out.println("JSON file is not found");
            e. printStackTrace();


        }
        catch (ParseException e){
            System.out.println("Parse Execption occured");
            e.printStackTrace();
        }
        catch (IOException e){
            e.printStackTrace();
        }

    }

// ================ obtaining PR for each commit and saving them in a file ===================================
    public void obtainingThePRS() throws IOException{


        for(String commitHash: patchInformation_svnRevisionpublic){

            setUrlForObtainingPRs(commitHash);


            //calling the API calling method
            callingTheAPI(getUrlForObtainingPRs(),jsonOutPutFileOfPRs,true,false);
            savePRSInArray();
        }

        //        printing the lists of product Id and PrNo for the patch
        printTheResults();


    }

    //================================= saving the  PRs in the array========================================

    public void savePRSInArray(){
        try{
            JSONObject parentJsonObject= (JSONObject) parser.parse(new FileReader (location+jsonOutPutFileOfPRs));

            JSONArray jsonArray= (JSONArray)parentJsonObject.get("items");

            for(int i =0 ;i< jsonArray.size();i++){

                JSONObject  prNoForCommitID=(JSONObject)jsonArray.get(i);
                String state=(String)prNoForCommitID.get("state");


                if(state.equals("closed") ){

                    JSONObject prDetails=(JSONObject)prNoForCommitID.get("pull_request");
                    prHtmlUrlDetails= (String)prDetails.get("html_url");

                    // for printing the URL
                    // System.out.println(prHtmlUrlDetails);

                    //to filter only pull requests to the wso2 repository[ex:- search Pull requests for this commit hash d58219020a9de4d26a4c33e913521e145d1b46b1]

                    if(StringUtils.contains(prHtmlUrlDetails, "/wso2/")){

                        String part1= StringUtils.substringAfter(prHtmlUrlDetails,"/wso2/");                
                        String tempProductId=StringUtils.substringBefore(part1, "/pull/");



                        long tempPrNo= (Long)prNoForCommitID.get("number");


                        if(prNumber.contains(tempPrNo)){

                            int indexOfPrNo= prNumber.indexOf(tempPrNo);
                            //checking whether the same product Name is in the relevant index at productID list
                            if((productID.get(indexOfPrNo).equals(tempProductId))){

                                //nothing will be added to the lists
                            }
                            else{
                                // though the PR number is same, it is not on the same product ID. So adding the record to the lists
                                prNumber.add(tempPrNo);
                                productID.add(tempProductId);
                            }

                        }
                        else{
                            //if the prNumber is not in the prNumber list, records are added to the  list
                            prNumber.add(tempPrNo);
                            productID.add(tempProductId);


                        }

                    }


                }


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



    }

    // ========================== prints the productId the pr number array lists =============================
    public void printTheResults(){

        System.out.println(productID);
        System.out.println(prNumber);


    }



}
