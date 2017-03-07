package com.wso2.code.quality.matrices;


public class MainClass
{
    public static void main( String[] args ) throws Exception
    {

        //       passing the PMT token
      
        Reviewers object= new Reviewers();

        object.setToken("PMT");
        object.setData();

        object.getThePublicGitCommitId();
        
        //        passing the github token
        object.setToken("Github");

        object.obtainingRepoNamesForCommitHashes();

        
        object.findingReviewers();
            
        
    }
}
