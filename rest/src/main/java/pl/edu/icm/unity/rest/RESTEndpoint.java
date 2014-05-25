/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.rest;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.ws.rs.core.Application;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.binding.BindingFactoryManager;
import org.apache.cxf.jaxrs.JAXRSBindingFactory;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.utils.ResourceUtils;
import org.apache.cxf.transport.servlet.CXFNonSpringServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import eu.unicore.util.configuration.ConfigurationException;
import pl.edu.icm.unity.server.endpoint.AbstractEndpoint;
import pl.edu.icm.unity.server.endpoint.BindingAuthn;
import pl.edu.icm.unity.server.endpoint.WebAppEndpointInstance;
import pl.edu.icm.unity.types.endpoint.EndpointTypeDescription;

/**
 * JAX-RS (REST) endpoint based on CXF.
 * The implementations are required to implement a single method {@link #getApplication()}. The returned object must
 * extend JAX-RS {@link Application} and return all endpoint specific classes and/or objects. Note that the 
 * returned {@link Application} will be enriched with additional Unity-specific filters/interceptors 
 * to handle authentication.
 * 
 *  
 * @author K. Benedyczak
 */
public abstract class RESTEndpoint extends AbstractEndpoint implements WebAppEndpointInstance
{
	protected RESTEndpointProperties genericEndpointProperties;
	protected String servletPath;
	
	public RESTEndpoint(EndpointTypeDescription type, String servletPath)
	{
		super(type);
		this.servletPath = servletPath;
	}

	@Override
	protected void setSerializedConfiguration(String serializedState)
	{
		properties = new Properties();
		try
		{
			properties.load(new ByteArrayInputStream(serializedState.getBytes()));
			genericEndpointProperties = new RESTEndpointProperties(properties);
		} catch (Exception e)
		{
			throw new ConfigurationException("Can't initialize the the generic RESTful"
					+ " endpoint's configuration", e);
		}		
	}

	protected abstract Application getApplication();

	private void deployResources(Bus bus)
	{
		JAXRSServerFactoryBean sf = ResourceUtils.createApplication(getApplication(), false);
		sf.setBus(bus);
		
		JAXRSBindingFactory factory = new JAXRSBindingFactory();
		factory.setBus(bus);
		
		BindingFactoryManager manager = bus.getExtension(BindingFactoryManager.class);
		manager.registerBindingFactory(JAXRSBindingFactory.JAXRS_BINDING_ID, factory);
		
		sf.create();
	}
	
	@Override
	public ServletContextHandler getServletContextHandler()
	{
		ServletContextHandler context = new ServletContextHandler(ServletContextHandler.NO_SESSIONS);
		context.setContextPath(description.getContextAddress());
		
		CXFNonSpringServlet cxfServlet = new CXFNonSpringServlet();
		Bus bus = BusFactory.newInstance().createBus();
		cxfServlet.setBus(bus);
		
		ServletHolder servlet = new ServletHolder(cxfServlet);
		servlet.setName("services");
		servlet.setForcedPath("services");
		context.addServlet(servlet, servletPath + "/*");
		
		deployResources(bus);
		return context;
	}
	
	@Override
	public void updateAuthenticators(List<Map<String, BindingAuthn>> authenticators)
			throws UnsupportedOperationException
	{
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException();
	}
}
