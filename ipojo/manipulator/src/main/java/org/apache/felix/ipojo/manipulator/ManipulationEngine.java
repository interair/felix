/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.felix.ipojo.manipulator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.felix.ipojo.manipulation.InnerClassManipulator;
import org.apache.felix.ipojo.manipulation.Manipulator;

/**
 * A {@code ManipulationEngine} is responsible to drive the component's
 * classes manipulation.
 *
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class ManipulationEngine {

    /**
     * List of component types.
     */
    private List<ManipulationUnit> manipulationUnits = new ArrayList<ManipulationUnit>();

    /**
     * Error reporting.
     */
    private Reporter m_reporter;

    /**
     * Bytecode store.
     */
    private ResourceStore m_store;

    /**
     * The visitor handling output result.
     */
    private ManipulationVisitor manipulationVisitor;

    /**
     * Add information related to a discovered component that will be manipulated.
     * @param component additional component
     */
    public void addManipulationUnit(ManipulationUnit component) {
        manipulationUnits.add(component);
    }

    public void setManipulationVisitor(ManipulationVisitor manipulationVisitor) {
        this.manipulationVisitor = manipulationVisitor;
    }

    /**
     * @param reporter Feedback reporter.
     */
    public void setReporter(Reporter reporter) {
        this.m_reporter = reporter;
    }

    /**
     * Provides the bytecode store that allows to retrieve bytecode of the
     * component's related resources (inner classes for example).
     * @param store Helps to locate bytecode for classes.
     */
    public void setResourceStore(ResourceStore store) {
        this.m_store = store;
    }

    /**
     * Manipulates classes of all the given component's.
     */
    public void generate() {

        // Iterates over the list of discovered components
        // Note that this list includes components from metadata.xml AND from annotations

        for (ManipulationUnit info : manipulationUnits) {

            byte[] bytecode;
            try {
                bytecode = m_store.read(info.getResourcePath());
            } catch (IOException e) {
                m_reporter.error("Cannot find bytecode for class '" + info.getClassName() + "': no bytecode found.");
                return;
            }

            // Is the visitor interested in this component ?
            ManipulationResultVisitor result = manipulationVisitor.visitManipulationResult(info.getComponentMetadata());

            if (result != null) {
                // Should always be the case

                // Manipulate the original bytecode and store the modified one
                Manipulator manipulator = new Manipulator();
                try {
                    byte[] out = manipulator.manipulate(bytecode);
                    // Call the visitor
                    result.visitClassStructure(manipulator.getManipulationMetadata());
                    result.visitManipulatedResource(info.getResourcePath(), out);
                } catch (IOException e) {
                    m_reporter.error("Cannot manipulate the class " + info.getClassName() + " : " + e.getMessage());
                    return;
                }


                // Visit inner classes
                for (String inner : manipulator.getInnerClasses()) {
                    // Get the bytecode and start manipulation
                    String resourcePath = inner + ".class";
                    String outerClassInternalName = info.getClassName().replace('.', '/');
                    byte[] innerClassBytecode;
                    try {
                        innerClassBytecode = m_store.read(resourcePath);
                    } catch (IOException e) {
                        m_reporter.error("Cannot find inner class '" + resourcePath + "'");
                        return;
                    }

                    // Manipulate inner class
                    try {
                        InnerClassManipulator innerManipulator = new InnerClassManipulator(outerClassInternalName, manipulator.getFields().keySet());
                        byte[] manipulated = innerManipulator.manipulate(innerClassBytecode);
                        // Propagate manipulated resource
                        result.visitManipulatedResource(resourcePath, manipulated);
                    } catch (IOException e) {
                        m_reporter.error("Cannot manipulate inner class '" + resourcePath + "'");
                        return;
                    }
                }

                // All resources have been manipulated for this component
                result.visitEnd();
            }
        }
    }
}