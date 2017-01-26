/*
 *  Copyright (c) Jan 20, 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

import java.io.FileReader;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;

import javax.net.ssl.HttpsURLConnection;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class GettingTheInfoOnPrReview extends GettingTheCommitIDFromPatchID {


    private String pullURL;
    private String urlReviews;
    private String pullOutPutFile="pullOutput.json";
    private String pullReviewOutPutFile="pullReviewOutPutFile.json";
    private String authorOfPR;
    private String tempProductID;
    private long tempPrNumber;

    private Long tempPrArray[];
    private String tempProductArray[];


    //    Lists for saving approved and commented users on the given pull request
    ArrayList <String> apprvedUsersList= new ArrayList<String>();
    ArrayList <String> commentedUsersList = new ArrayList<String>();


//========================= loop through all the elements in the lists ================================
    
//    converting the arraylists in to arrays for easy manipulation
    public void loopThroughLists() throws Exception{
        
        tempProductArray=new String[productID.size()];
        productID.toArray(tempProductArray);

        tempPrArray= new Long[prNumber.size()];
        prNumber.toArray(tempPrArray);

        // getting reviews on each Pull request
        for(int i=0; i<tempProductArray.length;i++){
            
            tempProductID=tempProductArray[i];
            tempPrNumber=tempPrArray[i];

            setURL();
            getReviewsForPR();
            getAuthorOfPR();


        }





    }

    //  ============================= Set URL for review API========================================================================

    public void setURL(){

        urlReviews="https://api.github.com/repos/wso2/"+tempProductID+"/pulls/"+tempPrNumber+"/reviews";

        //------------------------testing-------------------------------------------------------------------
        //        pullURL="https://api.github.com/repos/wso2/"+"product-is"+"/pulls/"+"885";
        //        urlReviews="https://api.github.com/repos/wso2/"+"product-is"+"/pulls/"+"885"+"/reviews";
        //--------------------------------------------------------------------------------------------------

    }

    //=========================== get reviews for the PR ==============================================================


    public void getReviewsForPR() throws Exception{

//        saving the output from the review API for the relevant pull request
        super.callingTheAPI(urlReviews, pullReviewOutPutFile, false, true);     //passing require review true as the github review API is currently in preview mode 



        //-------------------reading the json file received from the review API ------------------------------------------------
        try{

            JSONArray jsonArray= (JSONArray)parser.parse(new FileReader(location+pullReviewOutPutFile));


            for (int i=0;i<jsonArray.size();i++){
                JSONObject jsonObject= (JSONObject)jsonArray.get(i);
                String state=(String)jsonObject.get("state");

                //checking the user who approved the PR
                if (state.equals("APPROVED")){
                    JSONObject jsonObjtApprvdUserDetails=(JSONObject) jsonObject.get("user");
                    String userNameApprved= (String)jsonObjtApprvdUserDetails.get("login"); 
                    apprvedUsersList.add(userNameApprved);


                }
                //checking the user who commented on the PR

                else if (state.equals("COMMENTED")){
                    JSONObject jsonObjctCommnt = (JSONObject)jsonObject.get("user");
                    String commentedUserName= (String)jsonObjctCommnt.get("login");
                    commentedUsersList.add(commentedUserName);

                }

            }

        }
        catch(Exception e){
            e.printStackTrace();

        }

    }

    //  ================================== getting the author of the PR =======================================
    public void getAuthorOfPR() throws Exception {

        //getting the minimum Pull request no as it is the pull request which introduced the relevant commits to the repository.
        long minPrNo= Collections.min(prNumber);
        //finding the productId relevant to above pull request no
        String productIdOfLeastPr=productID.get(prNumber.indexOf(minPrNo));




        pullURL="https://api.github.com/repos/wso2/"+productIdOfLeastPr+"/pulls/"+minPrNo;

        super.callingTheAPI(pullURL, pullOutPutFile, false, false);

        //-------------------reading the json file thus saved------------------------------------------------

        try{
            JSONObject jsonObject=(JSONObject)parser.parse(new FileReader (location+pullOutPutFile));
            JSONObject userDetailsJSONObject= (JSONObject) jsonObject.get("user");
            authorOfPR= (String)userDetailsJSONObject.get("login");

        }
        catch(Exception e){

            e.printStackTrace();
        }

    }


    //====================== printing the arraylist elements========================================================

    public void printResults(){

        System.out.println("Author of the relevant commits : "+authorOfPR+"\n");

        System.out.println("The users who approved the commits in the Patch "+getPatchId());
        System.out.println(apprvedUsersList);

        System.out.println("\nThe users who commented on the commits in the Patch "+getPatchId());
        System.out.println(commentedUsersList+"\n");

    }


    //========================== to check whether the PR is merged or not=================================================
    public void checkPRMergedOrNot() throws IOException {

        String toCheckPrMergedOrNotURL="https://api.github.com/repos/wso2/"+tempProductID+"/pulls/"+tempPrNumber+"/merge";

        CloseableHttpClient httpClient= null;
        CloseableHttpResponse httpResponse= null;


        try{
            httpClient= HttpClients.createDefault();
            HttpGet httpGet= new HttpGet(toCheckPrMergedOrNotURL);
            httpResponse= httpClient.execute(httpGet);

            int responseCode= httpResponse.getStatusLine().getStatusCode();

            if(responseCode==204){
                System.out.println("This PR is merged");
            }
            else if(responseCode==404){
                System.out.println("This PR is merged");
            }


        }
        catch(ClientProtocolException e){
            e.printStackTrace();
        }
        catch(IOException e){
            e.printStackTrace();
        }

        finally{

            if(httpResponse != null){
                httpResponse.close();
            }
            if(httpClient != null){
                httpClient.close();
            }
        }


    }

}








