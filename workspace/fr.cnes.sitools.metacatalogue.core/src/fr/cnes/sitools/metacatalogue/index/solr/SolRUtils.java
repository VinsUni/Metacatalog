 /*******************************************************************************
 * Copyright 2010-2014 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.sitools.metacatalogue.index.solr;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.client.solrj.response.SolrPingResponse;
import org.apache.solr.core.CoreContainer;
import org.restlet.engine.Engine;

/**
 * The Class SolRUtils.
 * <P>
 * Explanation goes here.
 * <P>
 * 
 * @author m.gond
 */
public final class SolRUtils {

  /** The logger. */
  private static Logger logger = Engine.getLogger(SolRUtils.class.getName());

  /**
   * Instantiates a new SolR utils.
   */
  private SolRUtils() {
    super();
  }

  /**
   * Gets the SolR server.
   * 
   * @param solrUrl
   *          the solr url
   * @return the SolR server
   */
  public static SolrServer getSolRServer(String solrUrl) {
    SolrServer solrServer;
    HttpSolrServer server = new HttpSolrServer(solrUrl);
    server.setSoTimeout(20000); // socket read timeout
    server.setConnectionTimeout(10000);
    server.setDefaultMaxConnectionsPerHost(100);
    server.setMaxTotalConnections(100);
    server.setFollowRedirects(false); // defaults to false
    server.setMaxRetries(1); // defaults to 0. > 1 not recommended.
    solrServer = checkServer(server);
    if (solrServer != null) {
      logger.info("First SolR valid connection");
    }
    return solrServer;
  }

  /**
   * Gets the SolR server.
   * 
   * @param solrUrl
   *          the solr url
   * @return the SolR server
   */
  public static SolrServer getSolRServerWithoutCheck(String solrUrl) {
    SolrServer solrServer;
    HttpSolrServer server = new HttpSolrServer(solrUrl);
    server.setSoTimeout(20000); // socket read timeout
    server.setConnectionTimeout(10000);
    server.setDefaultMaxConnectionsPerHost(100);
    server.setMaxTotalConnections(100);
    server.setFollowRedirects(false); // defaults to false
    server.setMaxRetries(1); // defaults to 0. > 1 not recommended.
    solrServer = server;
    if (solrServer != null) {
      logger.info("First SolR valid connection");
    }
    return solrServer;
  }

  /**
   * Check server.
   */
  private static SolrServer checkServer(SolrServer solrServer) {
    if (solrServer != null) {
      try {
        SolrPingResponse ping = solrServer.ping();
        if (0 != ping.getStatus()) {
          solrServer = null;
        }
      }
      catch (SolrServerException e) {
        logger.log(Level.WARNING, "SolR Sever not reachable !", e);
        solrServer = null;
      }
      catch (IOException e) {
        logger.log(Level.WARNING, "SolR Sever available !", e);
        solrServer = null;
      }
    }
    return solrServer;
  }

  /**
   * 
   * @param solrHome
   * @param configFileName
   * @return
   */
  public static SolrServer getEmbeddedSolRServer(String solrHome, String configFileName, String coreName) {
    SolrServer solrServer;
    File configFile = new File(solrHome + "/" + configFileName);
    
    CoreContainer coreContainer = CoreContainer.createAndLoad(solrHome, configFile);
    EmbeddedSolrServer server = new EmbeddedSolrServer(coreContainer, coreName);
    solrServer = server;
    solrServer = checkServer(server);
    if (solrServer != null) {
      logger.info("First SolR valid connection");
    }
    return solrServer;
  }
}
