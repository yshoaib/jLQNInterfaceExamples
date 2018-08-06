package ca.appsimulations.examples;

import ca.appsimulations.jlqninterface.lqn.entities.ActivityDefBase;
import ca.appsimulations.jlqninterface.lqn.entities.ActivityPhases;
import ca.appsimulations.jlqninterface.lqn.model.LqnModel;
import ca.appsimulations.jlqninterface.lqn.model.LqnXmlDetails;
import ca.appsimulations.jlqninterface.lqn.model.SolverParams;
import ca.appsimulations.jlqninterface.lqn.model.handler.LqnSolver;
import ca.appsimulations.jlqninterface.lqn.model.parser.LqnInputParser;
import ca.appsimulations.jlqninterface.lqn.model.parser.LqnResultParser;
import ca.appsimulations.jlqninterface.lqn.model.writer.LqnModelWriter;

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.List;

import static ca.appsimulations.jlqninterface.utilities.FileHandler.getResourceFile;
import static java.util.stream.Collectors.toList;

@Slf4j
public class Model {
    public static void main(String[] args) throws Exception {

        String inputFile = "input.lqnx";
        File intermediateInputFile = new File("intermediateInputFile.lqnx");
        File outputFile = new File("output.lqxo");
        File outputPs = new File("output.ps");
        outputFile.delete();
        outputPs.delete();

        int i =1;
        int users = 1;
        do{

            //read model
            LqnModel lqnModel = new LqnModel();
            new LqnInputParser(lqnModel, true).parseFile(getResourceFile(inputFile).getAbsolutePath());

            //set user count
            lqnModel.entryByName("load").getTask().setMultiplicity(users);
            log.info("Users: " + lqnModel.entryByName("load").getTask().getMutiplicityString());

//            log.info("host demand of load is: " + lqnModel.entryByName("load").getEntryPhaseActivities()
//                    .getActivityAtPhase(1)
//                    .getHost_demand_mean());


            //write intermediate input
            new LqnModelWriter().write(lqnModel, intermediateInputFile.getAbsolutePath());


            //solve
            boolean solveResult =
                    LqnSolver.solveLqns(intermediateInputFile.getAbsolutePath(),
                                        new LqnResultParser(new LqnModel()),
                                        outputFile.getAbsolutePath());

            //was the model solved successfully
            if (solveResult == false) {
                log.error("problem solving lqn model");
                return;
            }

            //find response time
            LqnModel lqnModelResult = new LqnModel();
            new LqnResultParser(lqnModelResult).parseFile(outputFile.getAbsolutePath());
            log.info("response time of load is: " + getResponseTime(lqnModelResult, "load"));

            LqnSolver.savePostScript(outputFile.getAbsolutePath(),
                                     outputPs.getAbsolutePath());

            users = i * 10; //users = 1, 10, 20, ..., 100
            i++;

        }while(i <= 10);
    }

    public static double getResponseTime( LqnModel lqnModelResult, String entryName) {
        String activityName = lqnModelResult.entryByName(entryName).getEntryPhaseActivities().getActivityAtPhase(1)
                .getName();
        List<ActivityDefBase> activities =
                lqnModelResult.activities().stream().filter(activityDefBase -> activityDefBase.getName().equals
                        (activityName)).collect(
                        toList());

        double responseTime = 0;
        if (activities.size() > 0) {
            ActivityPhases ap = (ActivityPhases) activities.get(0);
            responseTime = ap.getResult().getService_time();
        }
        return responseTime;
    }
}
