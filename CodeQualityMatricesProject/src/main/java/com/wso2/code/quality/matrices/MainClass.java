package com.wso2.code.quality.matrices;


public class MainClass {
    public static void main(String[] args) throws Exception {

        //       passing the PMT token

        CallingAPI object = new CallingAPI();

        object.setToken("PMT");
        String[] commitsInTheGivenPatch = object.setData();

        //object.getThePublicGitCommitId();

        //        passing the github token
        object.setToken("Github");

      //  object.obtainingRepoNamesForCommitHashes();


      //  object.findingReviewers();


    }
}
