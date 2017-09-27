/*
 * Copyright (c) 2017 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.plugins;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.obiba.runtime.Version;

/**
 * Plugin description that is get from the update site.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class PluginPackage {
  private final String name;
  private final String type;
  private final String title;
  private final String description;
  private final Version version;
  private final Version opalVersion;
  private final Version micaVersion;
  private final Version agateVersion;
  private final String fileName;

  public PluginPackage(@JsonProperty("name") String name,
                       @JsonProperty("type") String type,
                       @JsonProperty("title") String title,
                       @JsonProperty("description") String description,
                       @JsonProperty("version") String version,
                       @JsonProperty("opalVersion") String opalVersion,
                       @JsonProperty("micaVersion") String micaVersion,
                       @JsonProperty("agateVersion") String agateVersion,
                       @JsonProperty("file") String fileName) {
    this.name = name;
    this.type = type;
    this.title = title;
    this.description = description;
    this.version = new Version(version);
    this.opalVersion = (opalVersion == null || opalVersion.isEmpty()) ? null : new Version(opalVersion);
    this.micaVersion = (micaVersion == null || micaVersion.isEmpty()) ? null : new Version(micaVersion);
    this.agateVersion = (agateVersion == null || agateVersion.isEmpty()) ? null : new Version(agateVersion);
    this.fileName = fileName;
  }

  public String getName() {
    return name;
  }

  public String getType() {
    return type;
  }

  public String getTitle() {
    return title;
  }

  public String getDescription() {
    return description;
  }

  public Version getVersion() {
    return version;
  }

  public boolean hasOpalVersion() {
    return opalVersion != null;
  }

  public Version getOpalVersion() {
    return opalVersion;
  }

  public boolean hasMicaVersion() {
    return micaVersion != null;
  }

  public Version getMicaVersion() {
    return micaVersion;
  }

  public boolean hasAgateVersion() {
    return agateVersion != null;
  }

  public Version getAgateVersion() {
    return agateVersion;
  }

  public Version getHostVersion() {
    if (hasOpalVersion()) return getOpalVersion();
    if (hasMicaVersion()) return getMicaVersion();
    if (hasAgateVersion()) return getAgateVersion();
    return new Version("0.0.0");
  }

  public String getFileName() {
    return fileName;
  }

  public boolean isSameAs(String name) {
    return this.name.equals(name);
  }

  public boolean isSameAs(String name, Version version) {
    return this.name.equals(name) && this.version.equals(version);
  }

  public boolean isNewerThan(String name, Version version) {
    return this.name.equals(name) && this.version.compareTo(version)>0;
  }
}
