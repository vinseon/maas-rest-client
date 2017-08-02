/*
 * ProActive Parallel Suite(TM):
 * The Open Source library for parallel and distributed
 * Workflows & Scheduling, Orchestration, Cloud Automation
 * and Big Data Analysis on Enterprise Grids & Clouds.
 *
 * Copyright (c) 2007 - 2017 ActiveEon
 * Contact: contact@activeeon.com
 *
 * This library is free software: you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation: version 3 of
 * the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 * If needed, contact us to obtain a release under GPL Version 2 or 3
 * or a different license than the AGPL.
 */
package org.ow2.proactive.connector.maas.data;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;


/**
 * @author ActiveEon Team
 * @since 10/01/17
 */
@Getter(AccessLevel.PUBLIC)
@ToString
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Interface {

    @JsonProperty("effective_mtu")
    private Long effectiveMtu;

    @JsonProperty("resource_uri")
    private String resourceUri;

    @JsonProperty("name")
    private String name;

    @JsonProperty("parents")
    private Object[] parents;

    @JsonProperty("discovered")
    private DiscoveredInterface[] discovered;

    @JsonProperty("type")
    private String type;

    @JsonProperty("tags")
    private String[] tags;

    @JsonProperty("params")
    private String params;

    @JsonProperty("links")
    private Link[] links;

    @JsonProperty("children")
    private Object[] children;

    @JsonProperty("vlan")
    private Vlan vlan;

    @JsonProperty("id")
    private Long id;

    @JsonProperty("mac_address")
    private String macAddress;

    @JsonProperty("enabled")
    private Boolean enabled;
}
