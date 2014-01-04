/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.test.headlessui;

import static org.junit.Assert.*;

import org.junit.Test;
import org.openqa.selenium.By;

/**
 * 
 * @author K. Benedyczak
 */
public class TestBasic extends SeleniumTestBase
{
	private String baseUrl = "https://localhost:2443/admin/admin";

	@Test
	public void loginTest() throws Exception
	{
		driver.get(baseUrl + "/admin/admin");
		driver.findElement(By.id("AuthenticationUI.username")).clear();
		driver.findElement(By.id("AuthenticationUI.username")).sendKeys("a");
		driver.findElement(By.id("WebPasswordRetrieval.password")).clear();
		driver.findElement(By.id("WebPasswordRetrieval.password")).sendKeys("a");
		driver.findElement(By.id("AuthenticationUI.authnenticateButton")).click();
		assertTrue(driver.findElement(By.id("MainHeader.loggedAs")).getText().contains("[entity id: 1]"));
		driver.findElement(By.id("MainHeader.logout"));
	}
}
