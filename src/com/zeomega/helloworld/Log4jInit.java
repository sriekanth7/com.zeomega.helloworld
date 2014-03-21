package com.zeomega.helloworld;

import java.io.IOException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.PropertyConfigurator;

/**
 * Servlet implementation class Log4jInit
 */
@WebServlet("/Log4jInit")
public class Log4jInit extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
	public void init(ServletConfig config) {
		System.out.println("Log4J for ZeOmega Helloworld entities initializing");
		String log4jLocation = config.getInitParameter("log4j-properties-location");
		System.out.println("Initializing log properties from " + log4jLocation);
		ServletContext sc = config.getServletContext();
		String webAppPath = sc.getRealPath("/");
		String log4jProp = webAppPath + log4jLocation;
		PropertyConfigurator.configure(log4jProp);
	}
}
