/* *********************************************************************** *
 * project: org.matsim.*
 * QSimFactory.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package org.matsim.ptproject.qsim;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.events.SynchronizedEventsManagerImpl;
import org.matsim.core.mobsim.framework.MobsimFactory;
import org.matsim.pt.qsim.ComplexTransitStopHandlerFactory;
import org.matsim.pt.qsim.TransitAgentFactory;
import org.matsim.pt.qsim.TransitQSimEngine;
import org.matsim.ptproject.qsim.agents.AgentFactory;
import org.matsim.ptproject.qsim.agents.DefaultAgentFactory;
import org.matsim.ptproject.qsim.agents.PopulationAgentSource;
import org.matsim.ptproject.qsim.interfaces.Netsim;
import org.matsim.ptproject.qsim.qnetsimengine.DefaultQSimEngineFactory;
import org.matsim.ptproject.qsim.qnetsimengine.ParallelQNetsimEngineFactory;
import org.matsim.ptproject.qsim.qnetsimengine.QNetsimEngineFactory;

/**
 * The MobsimFactory is necessary so that something can be passed to the controler which instantiates this.
 * Can (presumably) be something much more minimalistic than QSimI.  kai, jun'10
 *
 * @author dgrether
 *
 */
public class QSimFactory implements MobsimFactory {

    private final static Logger log = Logger.getLogger(QSimFactory.class);

    @Override
    public Netsim createMobsim(Scenario sc, EventsManager eventsManager) {

        QSimConfigGroup conf = sc.getConfig().getQSimConfigGroup();
        if (conf == null) {
            throw new NullPointerException("There is no configuration set for the QSim. Please add the module 'qsim' to your config file.");
        }

        // Get number of parallel Threads
        int numOfThreads = conf.getNumberOfThreads();
        QNetsimEngineFactory netsimEngFactory;
        if (numOfThreads > 1) {
            eventsManager = new SynchronizedEventsManagerImpl(eventsManager);
            netsimEngFactory = new ParallelQNetsimEngineFactory();
            log.info("Using parallel QSim with " + numOfThreads + " threads.");
        } else {
            netsimEngFactory = new DefaultQSimEngineFactory();
        }
        QSim qSim = new QSim(sc, eventsManager, netsimEngFactory);
        AgentFactory agentFactory;
        
        if (sc.getConfig().scenario().isUseTransit()) {
            agentFactory = new TransitAgentFactory(qSim);
            TransitQSimEngine transitEngine = new TransitQSimEngine(qSim);
            transitEngine.setUseUmlaeufe(true);
            transitEngine.setTransitStopHandlerFactory(new ComplexTransitStopHandlerFactory());
            qSim.addDepartureHandler(transitEngine);
            qSim.addAgentSource(transitEngine);
            qSim.addMobsimEngine(transitEngine);
        } else {
            agentFactory = new DefaultAgentFactory(qSim);
        }
        
        PopulationAgentSource agentSource = new PopulationAgentSource(sc.getPopulation(), agentFactory, qSim);
        qSim.addAgentSource(agentSource);
        return qSim;
    }

}
