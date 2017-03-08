package com.wso2.code.quality.matrices;


import com.sun.org.apache.xpath.internal.SourceTree;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Scanner;

public class MainClass {
    public static void main(String[] args) throws Exception {
        Scanner user_input = new Scanner(System.in);
        System.out.println("Enter PMT Token");
        String pmtToken = user_input.next();

        System.out.println("Enter the patch id");
        String patchId = user_input.next();

        String pmtUrl = "http://umt.private.wso2.com:9765/codequalitymatricesapi/1.0.0//properties?path=/_system/governance/patchs/" + patchId;


        CallingAPI callingAPI = new CallingAPI();
        JSONArray jsonArray = (JSONArray) callingAPI.callingTheAPI(pmtUrl, null, pmtToken, false, false);


        Pmt pmt = new Pmt();
        String[] commitsInTheGivenPatch = pmt.getThePublicGitCommitId(jsonArray);

        //        passing the github token
        System.out.println("Enter Github token");
        String gitHubToken = user_input.next();


        BlameCommit blameCommit = new BlameCommit();
        blameCommit.obtainingRepoNamesForCommitHashes(gitHubToken, commitsInTheGivenPatch, callingAPI);


        //       passing the PMT token


        // callingAPI.setToken("PMT");
//        String[] commitsInTheGivenPatch = callingAPI.setData();

        //callingAPI.getThePublicGitCommitId();


        //  callingAPI.obtainingRepoNamesForCommitHashes();


        //  callingAPI.findingReviewers();


    }


}
