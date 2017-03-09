package com.wso2.code.quality.matrices;

import org.json.JSONArray;

import java.util.Set;

/**
 * This is the class having the main method of this application
 * PMT Access token, patch id and github access token
 * should be passed as command line arguments when running the application
 */
public class MainClass {
    public static void main(String[] args) {

        String pmtToken = args[0];
        String patchId = args[1];

        String pmtUrl = "http://umt.private.wso2.com:9765/codequalitymatricesapi/1.0.0//properties?path=/_system/governance/patchs/" + patchId;

        RestApiCaller restApiCaller = new RestApiCaller();
        JSONArray jsonArray = (JSONArray) restApiCaller.callingTheAPI(pmtUrl, pmtToken, false, false);

        Pmt pmt = new Pmt();
        String[] commitsInTheGivenPatch = pmt.getThePublicGitCommitId(jsonArray);

        String gitHubToken = args[2];

        BlameCommit blameCommit = new BlameCommit();
        Set<String> commitHashObtainedForPRReview = blameCommit.obtainingRepoNamesForCommitHashes(gitHubToken, commitsInTheGivenPatch, restApiCaller);

        Reviewers reviewers = new Reviewers();
        reviewers.findingReviewers(commitHashObtainedForPRReview, gitHubToken);


    }


}
