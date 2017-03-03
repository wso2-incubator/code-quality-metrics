package kasun.test.testingMaven;


import java.io.IOException;


public class App 
{
    public static void main( String[] args ) throws Exception
    {

        //       passing the PMT token
      
        GettingReviewers object= new GettingReviewers();

        object.setToken("PMT");
        object.setData();

        object.getThePublicGitCommitId();
        
        //        passing the github token
        object.setToken("Github");

        object.obtainingRepoNamesForCommitHashes();

        
        object.findingReviewers();
            
        
    }
}
