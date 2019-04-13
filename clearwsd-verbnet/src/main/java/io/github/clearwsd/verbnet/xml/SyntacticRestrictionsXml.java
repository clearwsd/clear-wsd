/*
 * Copyright 2019 James Gung
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.clearwsd.verbnet.xml;

import io.github.clearwsd.verbnet.SyntRes;
import io.github.clearwsd.verbnet.SyntResDescription;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * XML binding implementation of {@link SyntResDescription}.
 *
 * @author jgung
 */
@Data
@Accessors(fluent = true)
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = SyntacticRestrictionsXml.ROOT_NAME)
public class SyntacticRestrictionsXml implements SyntResDescription {

    static final String ROOT_NAME = "SYNRESTRS";

    @XmlAttribute(name = "logic")
    private String logic = "";

    @XmlElement(name = SyntacticRestrictionXml.ROOT_NAME)
    private List<SyntacticRestrictionXml> syntacticRestrictions = new ArrayList<>();

    @Override
    public List<SyntRes> restrictions() {
        return syntacticRestrictions.stream()
            .map(res -> (SyntRes) res)
            .collect(Collectors.toList());
    }
}
