package edu.northwestern.amq;


import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PropUtility {

	private static String S_ENV_KEY = "edu.northwestern.env";
	private static final String S_SOURCE_ENVIRONMENT_VARIABLE_NAME = "edu.northwestern.source";
	private static final String S_ENVIRONMENT_ENVIRONMENT_VARIABLE_NAME = "edu.northwestern.env";
	
	protected static final Logger infoLogger = LoggerFactory.getLogger("infoLogger");
	protected static final Logger errorLogger = LoggerFactory.getLogger("errorLogger");

	/**
	 * Convert String/List values to String[]
	 * 
	 * @param String
	 * @return Array
	 * @throws Exception
	 */
	public static String[] getListValues(String type) throws Exception {

		List<String> elementList = Arrays.asList(type.split(","));
		for (int i = 0; i < elementList.size(); i++) {
			elementList.set(i, elementList.get(i).trim());
		}
		String[] elementArray = elementList.toArray(new String[elementList.size()]);
		return elementArray;

	}

	/**
	 * 
	 * @param pProperties
	 * @param sAtrributeName
	 * @return
	 * @throws Exception
	 */
	public static String getApplicationSpecificProperty(Properties pProperties, String sAtrributeName) throws Exception {
		return getApplicationSpecificProperty(pProperties, sAtrributeName, null);
	}

	/**
	 * 
	 * @param pProperties
	 * @param sAtrributeName
	 * @param sDefaultValue
	 * @return
	 * @throws Exception
	 */
	public static String getApplicationSpecificProperty(Properties pProperties, String sAtrributeName, String sDefaultValue) throws Exception {
		String sApplicationName = System.getProperty(S_SOURCE_ENVIRONMENT_VARIABLE_NAME);

		infoLogger.debug("Property File = {}", pProperties);
		infoLogger.debug("Attribute Name = {}", sAtrributeName);
		infoLogger.debug("Application Name = {}", sApplicationName);
		infoLogger.debug("Default Value = {}", sDefaultValue);


		String sReturn = pProperties.getProperty(sApplicationName + "." + sAtrributeName, pProperties.getProperty(sAtrributeName, sDefaultValue));

		infoLogger.debug("Return Value = {}", sReturn);

		return sReturn;
	}

	/**
	 * 
	 * @param pProperties
	 * @param sAtrributeName
	 * @return
	 * @throws Exception
	 */
	public static String getEnvironmentSpecificProperty(Properties pProperties, String sAtrributeName) throws Exception {
		return getEnvironmentSpecificProperty(pProperties, sAtrributeName, null);
	}

	/**
	 * 
	 * @param pProperties
	 * @param sAtrributeName
	 * @param sDefaultValue
	 * @return
	 * @throws Exception
	 */
	public static String getEnvironmentSpecificProperty(Properties pProperties, String sAtrributeName, String sDefaultValue) throws Exception {
		String sEnvironmentName = System.getProperty(S_ENVIRONMENT_ENVIRONMENT_VARIABLE_NAME);

		infoLogger.debug("Property File = {}", pProperties);
		infoLogger.debug("Attribute Name = {}", sAtrributeName);
		infoLogger.debug("Environment Name = {}", sEnvironmentName);
		infoLogger.debug("Default Value = {}", sDefaultValue);

		String sReturn = pProperties.getProperty(sEnvironmentName + "." + sAtrributeName, pProperties.getProperty(sAtrributeName, sDefaultValue));

		infoLogger.debug("Return Value = {}", sReturn);

		return sReturn;
	}

	public static Properties loadPropsFromClasspath(String sPropertyFileName) throws Exception {
		return loadPropsFromClasspath(sPropertyFileName, true);
	}

	/**
	 * Returns an Properties object that contains name/value pairs of configuration data for the app. This method will
	 * first try to load the property file in the Classpath based on the file name passed into the method. It will then
	 * try to load an environment specific version of the file using a variable com.nu.env passed in at runtime. If the
	 * properties exist in both files the environment specific value will overwrite those in the default.
	 * <p>
	 * If at least one property file is not found an Exception will be thrown.
	 *
	 * @return Properties file containing application information
	 * @see Properties
	 * @throws Exception
	 */
	public static Properties loadPropsFromClasspath(String sPropertyFileName, boolean bFatal) throws Exception {
		if (sPropertyFileName != null) {
			sPropertyFileName = sPropertyFileName.replaceFirst(".properties", "");
		}

		// This value is passed in by the java command that starts the app.
		String sEnvironment = System.getProperty(S_ENV_KEY);
		if (sEnvironment == null) {
			sEnvironment = "";
		}

		Properties pLocal = new Properties();
		InputStream inputDefault = null;
		InputStream inputEnvSpecific = null;

		try {
			inputDefault = Thread.currentThread().getContextClassLoader().getResourceAsStream(sPropertyFileName + ".properties");
			
			if (inputDefault != null) {
				infoLogger.debug("Loading property file: {}.properties", sPropertyFileName);
				pLocal.load(inputDefault);
			}
			else {
				inputDefault = Thread.currentThread().getContextClassLoader().getResourceAsStream("resources/" + sPropertyFileName + ".properties");
				if (inputDefault != null) {
					infoLogger.debug("Loading property file: resources/{}.properties", sPropertyFileName);
					pLocal.load(inputDefault);
				}
			}

			/*
			 * Load the Environment specific property file. This will load the environment specific property file and
			 * overwrite any values that have the same key. i.e. if the default property file contained a key/value of
			 * log4jProperties and the IntegrationAppSub_dev.properties contained a key log4jProperties the value in the
			 * IntegrationAppSub_dev.properties would overwrite the value found in the default file.
			 */
			if (sEnvironment != null && sEnvironment.trim().length() > 0) {
				inputEnvSpecific = Thread.currentThread().getContextClassLoader().getResourceAsStream(sPropertyFileName + "_" + sEnvironment + ".properties");
				if (inputEnvSpecific != null) {
					infoLogger.debug("Loading property file: {}_{}.properties", sPropertyFileName, sEnvironment);
					pLocal.load(inputEnvSpecific);
				}
				else {
					inputEnvSpecific = Thread.currentThread().getContextClassLoader().getResourceAsStream("resources/" + sPropertyFileName + "_" + sEnvironment + ".properties");
					if(inputEnvSpecific != null) {
						infoLogger.debug("Loading property file: resources/{}_{}.properties", sPropertyFileName, sEnvironment);
						pLocal.load(inputEnvSpecific);
					}
				}
			}

			if (inputDefault == null && inputEnvSpecific == null && bFatal) {
				throw new Exception("Unable to load " + sPropertyFileName + " and " + sPropertyFileName + "_" + sEnvironment + " property files.");
			}
		}
		catch (IOException ex) {
			ex.printStackTrace();
			Exception e = new Exception("Unable to load " + sPropertyFileName + "property file.");
			e.initCause(ex);
			throw e;
		}
		finally {
			if (inputDefault != null) {
				try {
					inputDefault.close();
				}
				catch (IOException e) {
					e.printStackTrace();
				}
			}

			if (inputEnvSpecific != null) {
				try {
					inputEnvSpecific.close();
				}
				catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return pLocal;
	}

	/**
	 * 
	 * @param sValue to be tested
	 * @return true is the value passed in represents an affirmative ("true", "yes", or "1")
	 */
	public static boolean isTrue(String sValue) {
		boolean bIsTrue = false;

		if (sValue != null) {
			if (sValue.equalsIgnoreCase("true") || sValue.equalsIgnoreCase("yes") || sValue.equalsIgnoreCase("1")) {
				bIsTrue = true;
			}
		}
		return bIsTrue;
	}
}
