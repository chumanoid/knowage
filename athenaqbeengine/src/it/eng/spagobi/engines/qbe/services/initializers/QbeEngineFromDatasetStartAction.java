/* SpagoBI, the Open Source Business Intelligence suite

 * Copyright (C) 2012 Engineering Ingegneria Informatica S.p.A. - SpagoBI Competency Center
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0, without the "Incompatible With Secondary Licenses" notice.
 * If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package it.eng.spagobi.engines.qbe.services.initializers;

import it.eng.qbe.dataset.FederationUtils;
import it.eng.qbe.dataset.QbeDataSet;
import it.eng.spago.base.SourceBean;
import it.eng.spagobi.commons.bo.UserProfile;
import it.eng.spagobi.services.common.SsoServiceInterface;
import it.eng.spagobi.tools.dataset.bo.IDataSet;
import it.eng.spagobi.tools.dataset.bo.JDBCDataSet;
import it.eng.spagobi.tools.dataset.common.metadata.IFieldMetaData;
import it.eng.spagobi.tools.dataset.common.metadata.IMetaData;
import it.eng.spagobi.tools.dataset.persist.IDataSetTableDescriptor;
import it.eng.spagobi.tools.datasource.bo.IDataSource;
import it.eng.spagobi.utilities.assertion.Assert;
import it.eng.spagobi.utilities.engines.EngineConstants;
import it.eng.spagobi.utilities.exceptions.SpagoBIRuntimeException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * The Class QbeEngineFromDatasetStartAction. Called when opening QBE engine by passing a datase, not a document.
 *
 * @author Giulio Gavardi
 */
public class QbeEngineFromDatasetStartAction extends QbeEngineStartAction {

	// INPUT PARAMETERS

	// OUTPUT PARAMETERS
	public static final String LANGUAGE = "LANGUAGE";
	public static final String COUNTRY = "COUNTRY";

	// SESSION PARAMETRES
	public static final String ENGINE_INSTANCE = EngineConstants.ENGINE_INSTANCE;
	public static final String REGISTRY_CONFIGURATION = "REGISTRY_CONFIGURATION";

	// INPUT PARAMETERS

	// The passed dataset label
	public static final String DATASET_LABEL = "dataset_label";
	public static final String FEDERATED_DATASET = "federated";
	// label of default datasource associated to Qbe Engine
	public static final String DATASOURCE_LABEL = "datasource_label";

	/** Logger component. */
	private static transient Logger logger = Logger.getLogger(QbeEngineFromDatasetStartAction.class);

	public static final String ENGINE_NAME = "SpagoBIQbeEngine";

	private IDataSet dataSet;

	@Override
	public IDataSet getDataSet() {
		logger.debug("IN");
		if (dataSet == null) {
			// dataset information is coming with the request
			String datasetLabel = this.getAttributeAsString(DATASET_LABEL);
			logger.debug("Parameter [" + DATASET_LABEL + "]  is equal to [" + datasetLabel + "]");
			Assert.assertNotNull(datasetLabel, "Dataset not specified");
			dataSet = getDataSetServiceProxy().getDataSetByLabel(datasetLabel);
		}
		logger.debug("OUT");
		return dataSet;
	}

	@Override
	public IDataSource getDataSource() {
		logger.debug("IN");
		IDataSource datasource;
		if(super.getDataSource()==null){
			IDataSet dataset = this.getDataSet();
			datasource = dataset.getDataSource();

			if (datasource == null) {
				// if dataset has no datasource associated take it from request
				String dataSourceLabel = getSpagoBIRequestContainer().get(DATASOURCE_LABEL) != null ? getSpagoBIRequestContainer().get(DATASOURCE_LABEL).toString()
						: null;
				logger.debug("passed from server datasource " + dataSourceLabel);
				datasource = getDataSourceServiceProxy().getDataSourceByLabel(dataSourceLabel);
			}

		}else{
			datasource = super.getDataSource();
		}
		
		logger.debug("OUT : returning [" + datasource + "]");
		return datasource;
	}

	@Override
	public String getDocumentId() {
		// there is no document at the time
		return null;
	}

	// no template in this use case
	@Override
	public SourceBean getTemplateAsSourceBean() {
		SourceBean templateSB = null;
		return templateSB;
	}

	public IDataSet getDataSet(String datasetLabel) {
		logger.debug("IN");

		logger.debug("OUT");
		return getDataSetServiceProxy().getDataSetByLabel(datasetLabel);
	}

	@Override
	public Map addDatasetsToEnv() {
		String federatedDatasetId = this.getAttributeAsString(FEDERATED_DATASET);
		if(federatedDatasetId== null || federatedDatasetId.length()==0){
			logger.debug("Not Found a federated dataset on the request");
			return addSimpleDataSetToEnv();
		}
		logger.debug("Found a federated dataset on the request");
		return addFederatedDatasetsToEnv(federatedDatasetId);
	}

	public Map addSimpleDataSetToEnv() {
		Map env = super.getEnv();
		env.put(EngineConstants.ENV_LOCALE, getLocale());
		String datasetLabel = this.getAttributeAsString(DATASET_LABEL);
		env.put(EngineConstants.ENV_DATASET_LABEL, datasetLabel);

		IDataSet dataset = this.getDataSet();

		// substitute default engine's datasource with dataset one
		IDataSource dataSource = dataset.getDataSource();
		if (dataSource == null) {
			logger.debug("Dataset has no datasource.");
		} else {
			env.put(EngineConstants.ENV_DATASOURCE, dataSource);
		}

		IDataSetTableDescriptor descriptor = this.persistDataset(dataset, env);
		if (dataset instanceof QbeDataSet) {
			adjustMetadataForQbeDataset(dataset, descriptor);
		}

		List<IDataSet> dataSets = new ArrayList<IDataSet>();
		dataSets.add(dataset);
		env.put(EngineConstants.ENV_DATASETS, dataSets);
		return env;
	}

	public Map addFederatedDatasetsToEnv(String federatedDatasetId) {

		
		IDataSource cachedDataSource = getCacheDataSource();

		// substitute default engine's datasource with dataset one
		

		
		Map env = super.getEnv(cachedDataSource);
		env.put(EngineConstants.ENV_DATASOURCE, cachedDataSource);
		
		logger.debug(env);
		env.put(EngineConstants.ENV_LOCALE, getLocale());
		String datasetLabel = this.getAttributeAsString(DATASET_LABEL);


		

		if(federatedDatasetId!=null){
			// update parameters into the dataset
			logger.debug("The dataset is federated");
			logger.debug("Getting the configuration");

			String configurationJson = "";

			logger.debug("The configuration is " + configurationJson);

			List<IDataSet> dataSets = new ArrayList<IDataSet>();

			List<String> dsLabels = new ArrayList<String>();

			dsLabels.add("test1");
			dsLabels.add("test2");

			// update profile attributes into dataset
			Map<String, Object> userAttributes = new HashMap<String, Object>();
			Map<String, String> mapNameTable = new HashMap<String, String>();

			UserProfile profile = (UserProfile) this.getEnv().get(EngineConstants.ENV_USER_PROFILE);
			userAttributes.putAll(profile.getUserAttributes());
			userAttributes.put(SsoServiceInterface.USER_ID, profile.getUserId().toString());
			logger.debug("Setting user profile attributes into dataset...");
			logger.debug(userAttributes);


			//			DataSetPersister dsp = new DataSetPersister();
			//			try {
			//				dsp.cacheDataSets(dsLabels, getUserId());
			//			} catch (Exception e1) {
			//				logger.error("Error executing the service that persist the datasets on the cache",e1);
			//				throw new SpagoBIEngineRuntimeException("Error executing the service that persist the datasets on the cache",e1);
			//			}
			//		
			
			for (int i = 0; i < dsLabels.size(); i++) {

				String dsLabel = dsLabels.get(i);
				//adds the link between dataset and cached table name
				mapNameTable.put(dsLabel, dsLabel + "cached");

				IDataSet originalDataset = this.getDataSet(dsLabel);
				IDataSet cachedDataSet = FederationUtils.createDatasetOnCache(mapNameTable.get(dsLabel), originalDataset,cachedDataSource);
				cachedDataSet.setUserProfileAttributes(userAttributes);
				cachedDataSet.setPersistTableName(mapNameTable.get(dsLabel));
				cachedDataSet.setParamsMap(env);
				cachedDataSet.setDsMetadata(originalDataset.getDsMetadata());
				cachedDataSet.setDataSourceForReading(cachedDataSource);
				dataSets.add(cachedDataSet);
				


			}

			JSONObject relations = null;
			try {
				relations = new JSONObject(
						"{ 	relationships: [{ 		bidirectional: true, 		cardinality: 'many-to-one', 		sourceTable: { 			name: 'test1', 			className: 'test1' 		}, 		sourceColumns: ['customer_id'], 		destinationTable: { 			name: 'test2', 			className: 'test2' 		}, 		destinationColumns: ['store_id'] 	}] }");
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			FederationUtils.adjustRelationName(relations, mapNameTable);

			env.put(EngineConstants.ENV_RELATIONS, relations);

			env.put(EngineConstants.ENV_DATASET_CACHE_MAP,mapNameTable);
			env.put(EngineConstants.ENV_RELATIONS,relations);
			env.put(EngineConstants.ENV_DATASETS, dataSets);
			env.put(EngineConstants.ENV_DATASET_LABEL, datasetLabel);
			env.put(EngineConstants.ENV_DATASET_LABEL, datasetLabel);
			env.put(EngineConstants.ENV_FEDERATED_ID,federatedDatasetId);
		}

		return env;
	}

	

	/**
	 * Gets the datasource of the cache
	 * 
	 * @return
	 */
	private IDataSource getCacheDataSource() {
		String datasourceLabel = "cacheDataSource";// TO-DO chiamare servizio di SpagoBi /1.0/cache/datasource
		IDataSource dataSource = getDataSourceServiceProxy().getDataSourceByLabel(datasourceLabel);
		return dataSource;
	}

	/**
	 * This method solves the following issue: SQLDataSet defines the SQL statement directly considering the names' of the wrapped dataset fields, but, in case
	 * of QbeDataSet, the fields' names are "it.eng.spagobi......Entity.fieldName" and not the name of the persistence table!!! We modify the dataset's metadata
	 * in order to fix this.
	 *
	 * @param dataset
	 *            The persisted Qbe dataset
	 * @param descriptor
	 *            The persistence table descriptor
	 */
	// TODO move this logic inside the SQLDataSet: when building the
	// SQL statement, the SQLDataSet should get the columns' names
	// from the IDataSetTableDescriptor. Replace
	// IDataSet.getPersistTableName with
	// IDataSet.getPersistTableDescriptor in order to permit the
	// IDataSetTableDescriptor to go with its dataset.
	// TODO merge with it.eng.spagobi.engines.worksheet.services.initializers.WorksheetEngineStartAction.adjustMetadataForQbeDataset
	private void adjustMetadataForQbeDataset(IDataSet dataset, IDataSetTableDescriptor descriptor) {
		IMetaData metadata = dataset.getMetadata();
		int columns = metadata.getFieldCount();
		for (int i = 0; i < columns; i++) {
			IFieldMetaData fieldMetadata = metadata.getFieldMeta(i);
			String newName = descriptor.getColumnName(fieldMetadata.getName());
			fieldMetadata.setName(newName);
			fieldMetadata.setProperty("uniqueName", newName);
		}
		dataset.setMetadata(metadata);
	}

	@Override
	protected boolean tolerateMissingDatasource() {
		return true;
	}

}
