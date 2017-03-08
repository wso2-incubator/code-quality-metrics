package com.wso2.code.quality.matrices;

import org.json.JSONArray;
import java.util.Scanner;
import java.util.Set;

public class MainClass {
    public static void main(String[] args) {
      //  System.out.println("Enter PMT Token");
        String pmtToken = args[0];

       // System.out.println("Enter the patch id");
        String patchId = args[1];

        String pmtUrl = "http://umt.private.wso2.com:9765/codequalitymatricesapi/1.0.0//properties?path=/_system/governance/patchs/" + patchId;


        RestApiCaller restApiCaller = new RestApiCaller();
        JSONArray jsonArray = (JSONArray) restApiCaller.callingTheAPI(pmtUrl, pmtToken, false, false);


        Pmt pmt = new Pmt();
        String[] commitsInTheGivenPatch = pmt.getThePublicGitCommitId(jsonArray);

        //        passing the github token
       // System.out.println("Enter Github token");
        String gitHubToken = args[2];


        BlameCommit blameCommit = new BlameCommit();
        Set<String> commitHashObtainedForPRReview = blameCommit.obtainingRepoNamesForCommitHashes(gitHubToken, commitsInTheGivenPatch, restApiCaller);

        Reviewers reviewers = new Reviewers();
        reviewers.findingReviewers(commitHashObtainedForPRReview, gitHubToken);




    }


}
