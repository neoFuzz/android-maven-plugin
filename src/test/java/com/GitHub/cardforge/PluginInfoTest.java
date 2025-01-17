package com.github.cardforge;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.github.cardforge.maven.plugins.android.PluginInfo;
import org.junit.Test;

public class PluginInfoTest {
  
  @Test
  public void confirmGroupId()
  {
    assertEquals( "com.github.cardforge.maven.plugins", PluginInfo.getGroupId() );
  }

  @Test
  public void confirmArtifactId()
  {
    assertEquals( "android-maven-plugin", PluginInfo.getArtifactId() );
  }

  @Test
  public void confirmVersion()
  {
    assertNotNull( PluginInfo.getVersion() );
  }

  @Test
  public void confirmGav()
  {
    assertTrue( PluginInfo.getGAV()
        .startsWith( "com.github.cardforge.maven.plugins:android-maven-plugin:" ) );
  }
}
