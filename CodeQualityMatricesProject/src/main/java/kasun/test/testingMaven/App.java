package kasun.test.testingMaven;


import java.io.IOException;


public class App 
{
    public static void main( String[] args ) throws Exception
    {

        //       passing the PMT token
       
//        ---------------------------------------Code Main -------------
        GettingBlameCommit object= new GettingBlameCommit();

        object.setToken("PMT");
        object.setData();

        object.getThePublicGitCommitId();
        //        passing the github token
        object.setToken("Github");
//        object.obtainingThePRS();
        object.obtainingRepoNamesForCommitHashes();


//        System.out.println("Reviews on Pull requests");

//        ---------------------------------------
        
        //GettingTheInfoOnPrReview objectReview = new GettingTheInfoOnPrReview();
//        object.loopThroughLists();
//        object.printResults();
        //        object.checkPRMergedOrNot();
        
        
        
        
// ------------------ calling the graphql API--------------------
//        object.callingGraphqlApi();
        
        
        
    }
}
