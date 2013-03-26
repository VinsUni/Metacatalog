/*******************************************************************************
 * Copyright 2011 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
 * 
 * This file is part of SITools2.
 * 
 * SITools2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * SITools2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with SITools2.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package fr.cnes.sitools.metacatalogue.opensearch.indexer;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.restlet.Context;

import fr.cnes.sitools.metacatalogue.common.HarvesterStep;
import fr.cnes.sitools.metacatalogue.common.Metadata;
import fr.cnes.sitools.metacatalogue.exceptions.ProcessException;
import fr.cnes.sitools.metacatalogue.index.MetadataIndexer;
import fr.cnes.sitools.metacatalogue.index.solr.SolrMetadataIndexer;
import fr.cnes.sitools.metacatalogue.model.Fields;
import fr.cnes.sitools.metacatalogue.model.HarvestStatus;
import fr.cnes.sitools.metacatalogue.utils.CheckStepsInformation;
import fr.cnes.sitools.model.HarvesterModel;

public class OpensearchMetadataIndexer extends HarvesterStep {

  private int pageSize = 50;
  private MetadataIndexer solrIndexer;
  private Logger logger;
  private Context context;

  public OpensearchMetadataIndexer(HarvesterModel conf, Context context) {

    solrIndexer = new SolrMetadataIndexer(conf.getIndexerConf().getUrl(), context);
    this.context = context;

  }

  @Override
  public void execute(Metadata data) throws ProcessException {
    logger = context.getLogger();
    List<Fields> fields = data.getFields();
    try {
      logger.info("Add " + fields.size() + " metadata to the solrIndex cache");
      HarvestStatus status = (HarvestStatus) context.getAttributes().get("STATUS");
      status.setNbDocumentsIndexed(status.getNbDocumentsIndexed() + fields.size());

      for (Fields doc : fields) {
        solrIndexer.addFieldsToIndex(doc);
        if (solrIndexer.getCurrentNumberOfFieldsToIndex() == pageSize) {
          logger.info("Index the metadata in the solr server ");
          solrIndexer.indexMetadata();
        }
      }
    }
    catch (Exception e) {
      logger.log(Level.WARNING, e.getLocalizedMessage(), e);
      throw new ProcessException(e);
    }
    finally {
      if (next != null) {
        next.execute(data);
      }
    }
  }

  @Override
  public void end() {
    try {
      logger.info("Index the metadata in the solr server ");
      solrIndexer.indexMetadata();
    }
    catch (Exception e) {
      logger.log(Level.WARNING, e.getLocalizedMessage(), e);
    }
    finally {
      try {
        solrIndexer.commit();
      }
      catch (Exception e) {
        logger.log(Level.WARNING, "Cannot commit data", e);
      }
    }
  }

  @Override
  public CheckStepsInformation check() {
    CheckStepsInformation info = new CheckStepsInformation(true);
    if (next != null) {
      info = this.check();
    }
    if (!info.isOk()) {
      return info;
    }
    if (!solrIndexer.checkIndexerAvailable()) {
      info.setOk(false);
      info.setMessage("Solr server not reachable");
    }
    return info;

  }
}