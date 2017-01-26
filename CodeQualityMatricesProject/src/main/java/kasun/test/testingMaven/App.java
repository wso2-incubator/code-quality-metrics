package kasun.test.testingMaven;


import java.io.IOException;


public class App 
{
    public static void main( String[] args ) throws Exception
    {
        GettingTheInfoOnPrReview object= new GettingTheInfoOnPrReview();
        //       passing the PMT token
        object.setToken("PMT");
        object.setData();

        object.getThePublicGitCommitId();
        //        passing the github token
        object.setToken("Github");
        object.obtainingThePRS();


        System.out.println("Reviews on Pull requests");

        //GettingTheInfoOnPrReview objectReview = new GettingTheInfoOnPrReview();
        object.loopThroughLists();
        object.printResults();
        //        object.checkPRMergedOrNot();
    }
}
