package net.bhl.matsim.uam.run;

import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptorModule;
import com.google.common.collect.Sets;
import net.bhl.matsim.uam.config.UAMConfigGroup;
import net.bhl.matsim.uam.qsim.UAMQSimModule;
import net.bhl.matsim.uam.qsim.UAMSpeedModule;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.dvrp.run.DvrpModule;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.*;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ModeParams;
import org.matsim.core.config.groups.QSimConfigGroup.StarttimeInterpretation;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * The RunUAMScenario program start a MATSim run including Urban Air Mobility
 * capabilities.
 *
 * @author balacmi (Milos Balac), RRothfeld (Raoul Rothfeld)
 */
public class RunUAMScenario {

	private static UAMConfigGroup uamConfigGroup;
	private static CommandLine cmd;
	private static String path;
	private static Config config;
	private static Controler controler;
	private static Scenario scenario;
	private static String networkFilePath;
	private static String plansFilePath;
	private static String outputDirectory;
	private static int lastIteration;
	private static String transitScheduleFile;
	private static String transitVehiclesFile;

	public static void main(String[] args) {
		parseArguments(args);
		setConfig(path);
		createScenario();
		createControler(networkFilePath,plansFilePath, outputDirectory, lastIteration, transitScheduleFile, transitVehiclesFile).run();
	}

	public static void parseArguments(String[] args) {
		try {
			cmd = new CommandLine.Builder(args).allowOptions("config-path", "use-charging", "networkFilePath",
					"plansFilePath", "outputDirectory", "iteration", "transitScheduleFile", "transitVehiclesFile").build();

			if (cmd.hasOption("config-path"))
				path = cmd.getOption("config-path").get();
			else
				path = args[0];

			networkFilePath = args[2];
			plansFilePath = args[3];
			outputDirectory = args[4];
			lastIteration = Integer.parseInt(args[5]);
			transitScheduleFile = args[6];
			transitVehiclesFile = args[7];

		} catch (ConfigurationException e) {
			e.printStackTrace();
		}
		uamConfigGroup = new UAMConfigGroup();
	}

	public static Config createConfig() {
		return config = ConfigUtils.createConfig(uamConfigGroup, new DvrpConfigGroup());
	}

	public static Config setConfig(String path) {
		return config = ConfigUtils.loadConfig(path, uamConfigGroup, new DvrpConfigGroup());
	}

	public static Scenario createScenario() {
		scenario = ScenarioUtils.createScenario(config);
		ScenarioUtils.loadScenario(scenario);
		return scenario;
	}

	public static Scenario setScenario(Scenario scenario) {
		return RunUAMScenario.scenario = scenario;
	}

	public static Controler createControler(String networkFilePath, String plansFilePath, String outputDirectory, int lastIteration, String transitScheduleFile, String transitVehiclesFile) {
		try {
			cmd.applyConfiguration(config);
		} catch (ConfigurationException e) {
			e.printStackTrace();
		}

		controler = new Controler(scenario);

		controler.addOverridingModule(new DvrpModule());

		controler.addOverridingModule(new UAMModule(config));
		controler.addOverridingQSimModule(new UAMSpeedModule());
		controler.addOverridingModule(new SwissRailRaptorModule());

		controler.configureQSimComponents(configurator -> {
			UAMQSimModule.activateModes().configure(configurator);
		});

		controler.getConfig().transit().setUseTransit(true);
		controler.getConfig().transit().setUsingTransitInMobsim(true);
		controler.getConfig().qsim().setSimStarttimeInterpretation(StarttimeInterpretation.onlyUseStarttime);
		controler.getConfig().qsim().setStartTime(0.0);

		DvrpConfigGroup.get(config).setNetworkModesAsString("uam");
		config.planCalcScore().addModeParams(new ModeParams("access_uam_car"));
		config.planCalcScore().addModeParams(new ModeParams("egress_uam_car"));
		config.planCalcScore().addModeParams(new ModeParams("uam"));
		config.planCalcScore()
				.addActivityParams(new ActivityParams("uam_interaction").setScoringThisActivityAtAll(false));

		config.controler().setWriteEventsInterval(1);


		//From Base runMatsim


		//Config // Network, Plans, Controller

		config.controler().setCreateGraphs(false);

		config.global().setRandomSeed(4711);
		config.global().setCoordinateSystem("Atlantis");

		config.network().setInputFile(networkFilePath);
		config.plans().setInputFile(plansFilePath);
		config.controler().setOutputDirectory(outputDirectory);
		config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);

		config.controler().setFirstIteration(0);
		config.controler().setLastIteration(lastIteration);
		config.controler().setEventsFileFormats(Sets.newHashSet(ControlerConfigGroup.EventsFileFormat.valueOf("xml")));
		config.controler().setMobsim("qsim");


		//QSim
		config.qsim().setStartTime(0);
		config.qsim().setEndTime(30 * 60 * 60);
		config.qsim().setSnapshotPeriod(0);

		//Scoring
		config.planCalcScore().setLearningRate(1);
		config.planCalcScore().setBrainExpBeta(1);
		//Scoring Parameters
		config.planCalcScore().setLateArrival_utils_hr(-18);
		config.planCalcScore().setEarlyDeparture_utils_hr(0);
		config.planCalcScore().setPerforming_utils_hr(6);
		config.planCalcScore().setMarginalUtlOfWaiting_utils_hr(0);
		//modeParams
		config.planCalcScore().getOrCreateModeParams("car").setMarginalUtilityOfTraveling(-6.0);
		config.planCalcScore().getOrCreateModeParams("Bus").setMarginalUtilityOfTraveling(-6.0);
		config.planCalcScore().getOrCreateModeParams("walk").setMarginalUtilityOfTraveling(-6.0);
		//ActivityParams
		//home
		PlanCalcScoreConfigGroup.ActivityParams home = new PlanCalcScoreConfigGroup.ActivityParams("home");
		home.setPriority(1);
		home.setTypicalDuration(8 * 60 * 60); //Avg duration from Abit 100% = 7.03 hrs
		home.setMinimalDuration(6 * 60 * 60); //Avg duration from Abit 100% = 7.03 hrs
		config.planCalcScore().addActivityParams(home);
		//work
		PlanCalcScoreConfigGroup.ActivityParams work = new PlanCalcScoreConfigGroup.ActivityParams("work");
		work.setPriority(1);
		work.setTypicalDuration(7 * 60 * 60);  //Avg duration from Abit 100% = 7.08 hrs
		work.setMinimalDuration(6 * 60 * 60) ;      //Avg duration from Abit 100% = 7.08 hrs
        /*work.setOpeningTime(0);
        work.setLatestStartTime(0);
        work.setEarliestEndTime(0);
        work.setClosingTime(0);*/
		config.planCalcScore().addActivityParams(work);
		//accompany
		PlanCalcScoreConfigGroup.ActivityParams accompany = new PlanCalcScoreConfigGroup.ActivityParams("accompany");
		accompany.setPriority(1);
		accompany.setTypicalDuration(30 * 60); //Avg duration from Abit 100% = 0.31 hrs
        /*accompany.setMinimalDuration(0);
        accompany.setOpeningTime(0);
        accompany.setLatestStartTime(0);
        accompany.setEarliestEndTime(0);
        accompany.setClosingTime(0);*/
		config.planCalcScore().addActivityParams(accompany);
		//shopping
		PlanCalcScoreConfigGroup.ActivityParams shopping = new PlanCalcScoreConfigGroup.ActivityParams("shopping");
		shopping.setPriority(1);
		shopping.setTypicalDuration(0.5 * 60 * 60); //Avg duration from Abit 100% = 0.5 hrs
		shopping.setMinimalDuration(0.15 * 60 * 60); //Heuristic
        /*shopping.setOpeningTime(0);
        shopping.setLatestStartTime(0);
        shopping.setEarliestEndTime(0);
        shopping.setClosingTime(0);*/
		config.planCalcScore().addActivityParams(shopping);
		//recreation
		PlanCalcScoreConfigGroup.ActivityParams recreation = new PlanCalcScoreConfigGroup.ActivityParams("recreation");
		/*recreation.setPriority(1);*/
		recreation.setTypicalDuration(1.5 * 60 * 60); //Avg duration from Abit 100% = 1.6 hrs
		recreation.setMinimalDuration(0.5 * 60 * 60); //Heuristic
        /*recreation.setOpeningTime(0);
        recreation.setLatestStartTime(0);
        recreation.setEarliestEndTime(0);
        recreation.setClosingTime(0);*/
		config.planCalcScore().addActivityParams(recreation);
		//education
		PlanCalcScoreConfigGroup.ActivityParams education  = new PlanCalcScoreConfigGroup.ActivityParams("education");
		education.setPriority(1);
		education.setTypicalDuration(5 * 60 * 60); //Avg duration from Abit 100% = 5.65 hrs
		education.setMinimalDuration(2 * 60 * 60); //Heuristic
       /*education.setOpeningTime(0);
        education.setLatestStartTime(0);
        education.setEarliestEndTime(0);
        education.setClosingTime(0);*/
		config.planCalcScore().addActivityParams(education);
		//subtour
		PlanCalcScoreConfigGroup.ActivityParams subtour = new PlanCalcScoreConfigGroup.ActivityParams("subtour");
		/*subtour.setPriority(1);*/
		subtour.setTypicalDuration(0.5 * 60 * 60); //Avg duration from Abit 100% = 0.6 hrs
        /*subtour.setMinimalDuration(0);
        subtour.setOpeningTime(0);
        subtour.setLatestStartTime(0);
        subtour.setEarliestEndTime(0);
        subtour.setClosingTime(0);*/
		config.planCalcScore().addActivityParams(subtour);
		//other
		PlanCalcScoreConfigGroup.ActivityParams other = new PlanCalcScoreConfigGroup.ActivityParams("other");
		/*other.setPriority(1);*/
		other.setTypicalDuration(0.6 * 60 * 60);    //Avg duration from Abit 100% = 0.96 hrs
        /*other.setMinimalDuration(0);
        other.setOpeningTime(0);
        other.setLatestStartTime(0);
        other.setEarliestEndTime(0);
        other.setClosingTime(0);*/
		config.planCalcScore().addActivityParams(other);

		//Replanning
		config.strategy().setMaxAgentPlanMemorySize(5);
		config.strategy().setFractionOfIterationsToDisableInnovation(0.8); //Zame

		//Plan Selection or Mutation Strategies
		//SelectorSettings
		//BestScore
		StrategyConfigGroup.StrategySettings bestScore = new StrategyConfigGroup.StrategySettings();
		bestScore.setStrategyName(DefaultPlanStrategiesModule.DefaultSelector.BestScore);
		bestScore.setWeight(0.3);
		config.strategy().addStrategySettings(bestScore);
		//SelectRandom
		StrategyConfigGroup.StrategySettings selectRandom = new StrategyConfigGroup.StrategySettings();
		selectRandom.setStrategyName(DefaultPlanStrategiesModule.DefaultSelector.SelectRandom);
		selectRandom.setWeight(0.2);
		selectRandom.setDisableAfter(lastIteration/2);
		config.strategy().addStrategySettings(selectRandom);
		//StrategySettings
		//ReRoute
		StrategyConfigGroup.StrategySettings reRoute = new StrategyConfigGroup.StrategySettings();
		reRoute.setStrategyName(DefaultPlanStrategiesModule.DefaultStrategy.ReRoute);
		reRoute.setWeight(0.25);
		config.strategy().addStrategySettings(reRoute);
		//ChangeTripMode
		StrategyConfigGroup.StrategySettings changeSingleTripMode = new StrategyConfigGroup.StrategySettings();
		changeSingleTripMode.setStrategyName(DefaultPlanStrategiesModule.DefaultStrategy.ChangeSingleTripMode);
		changeSingleTripMode.setWeight(0.25);
		config.strategy().addStrategySettings(changeSingleTripMode);
		//ChangeMode
		config.changeMode().setModes(new String[]{"car", "Bus", "uam"});
		config.changeMode().setIgnoreCarAvailability(false);
		config.changeMode().setBehavior(ChangeModeConfigGroup.Behavior.fromAllModesToSpecifiedModes);



		//transit
		config.transit().setUseTransit(true);
		config.transit().setTransitScheduleFile(transitScheduleFile);
		config.transit().setVehiclesFile(transitVehiclesFile);
		config.transit().setTransitModes(Sets.newHashSet("pt","Bus"));



		//TeleportedModeParams

		PlansCalcRouteConfigGroup.ModeRoutingParams car_passenger = new PlansCalcRouteConfigGroup.ModeRoutingParams("Car_passenger");
		car_passenger.setTeleportedModeFreespeedFactor(1.0); //Same as car //car speed = 50 km/h = 13.89 m/s
		config.plansCalcRoute().addModeRoutingParams(car_passenger);


		/*PlansCalcRouteConfigGroup.ModeRoutingParams bike = new PlansCalcRouteConfigGroup.ModeRoutingParams(TransportMode.bike);
		bike.setTeleportedModeFreespeedFactor(2.0); //Double the travel time of car //car speed = 50 km/h = 13.89 m/s
		config.plansCalcRoute().addModeRoutingParams(bike);*/

		PlansCalcRouteConfigGroup.ModeRoutingParams train = new PlansCalcRouteConfigGroup.ModeRoutingParams("Rail");
		train.setTeleportedModeSpeed(18.05); // 65 km/h = 18.05 m/s
		train.setBeelineDistanceFactor(1.1);
		config.plansCalcRoute().addModeRoutingParams(train);

		PlansCalcRouteConfigGroup.ModeRoutingParams tram = new PlansCalcRouteConfigGroup.ModeRoutingParams("Tram");
		tram.setTeleportedModeSpeed(8.33); // 30 km/h = 8.33 m/s
		tram.setBeelineDistanceFactor(1.5);
		config.plansCalcRoute().addModeRoutingParams(tram);

		/*PlansCalcRouteConfigGroup.ModeRoutingParams walk = new PlansCalcRouteConfigGroup.ModeRoutingParams(TransportMode.walk);
		walk.setTeleportedModeSpeed(1.39); // 5 km/h = 1.39 m/s
		walk.setBeelineDistanceFactor(1.3);
		config.plansCalcRoute().addModeRoutingParams(walk);*/



		//Mode Scoring Parameters
		PlanCalcScoreConfigGroup.ModeParams modeParamsCar_passenger = config.planCalcScore().getOrCreateModeParams(car_passenger.getMode());
		modeParamsCar_passenger.setMarginalUtilityOfTraveling(-6.0);
		config.planCalcScore().addModeParams(modeParamsCar_passenger);

		PlanCalcScoreConfigGroup.ModeParams modeParamsTrain= config.planCalcScore().getOrCreateModeParams(train.getMode());
		modeParamsTrain.setMarginalUtilityOfTraveling(-6.0);
		config.planCalcScore().addModeParams(modeParamsTrain);

		PlanCalcScoreConfigGroup.ModeParams modeParamsTram = config.planCalcScore().getOrCreateModeParams(tram.getMode());
		modeParamsTram.setMarginalUtilityOfTraveling(-6.0);
		config.planCalcScore().addModeParams(modeParamsTram);

		//RoutingAlgorithm
		config.controler().setRoutingAlgorithmType(ControlerConfigGroup.RoutingAlgorithmType.SpeedyALT);




		//Changes for Downscaling
		config.qsim().setFlowCapFactor(0.05);
		config.qsim().setStorageCapFactor(0.105);

		//Saving bus from congestion according to https://github.com/matsim-org/matsim-code-examples/issues/395 & https://github.com/matsim-org/matsim-libs/pull/55
		//1% scenario I would set the pce to 0.03 or 0.05 for the transit vehicles.
		config.qsim().setPcuThresholdForFlowCapacityEasing(0.15);

		//Also we changed the PCE of Bus to 0.25 in the transit vehicles file. //Didn't work because the PCE should be smalled than the PCE threshold. Next.
		//static final String PCU_THRESHOLD_FOR_FLOW_CAPACITY_EASING = //
		//              "Flow capacity easing is activated for vehicles of size equal or smaller than the specified threshold. "
		//              + "Introduced to minimise the chances of buses being severely delayed in downsampled scenarios";
		// Setting PCE  to 0.15 for the transit vehicles in the transit vehicles file.
		// ---

		//Changes that should be verified
		/*config.qsim().setMainModes(Sets.newHashSet(TransportMode.car, "Bus"));*/

		config.plansCalcRoute().setAccessEgressType(PlansCalcRouteConfigGroup.AccessEgressType.accessEgressModeToLink);


		//


//////////////////////// end of base runMatsim ////////////////////////


		return controler;
	}
}
