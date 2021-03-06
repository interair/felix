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

package org.apache.felix.sigil.eclipse.internal.repository.eclipse;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarFile;

import org.apache.felix.sigil.common.core.BldCore;
import org.apache.felix.sigil.common.model.ModelElementFactory;
import org.apache.felix.sigil.common.model.ModelElementFactoryException;
import org.apache.felix.sigil.common.model.eclipse.ISigilBundle;
import org.apache.felix.sigil.common.model.osgi.IBundleModelElement;
import org.apache.felix.sigil.common.repository.AbstractBundleRepository;
import org.apache.felix.sigil.common.repository.IRepositoryVisitor;
import org.apache.felix.sigil.common.util.ManifestUtil;
import org.apache.felix.sigil.eclipse.SigilCore;
import org.apache.felix.sigil.eclipse.install.IOSGiInstall;
import org.eclipse.core.runtime.IPath;

public class OSGiInstallRepository extends AbstractBundleRepository
{

    private Map<String, List<ISigilBundle>> bundles;

    public OSGiInstallRepository(String id)
    {
        super(id);
    }

    public void refresh()
    {
        synchronized (this)
        {
            bundles = null;
        }

        notifyChange();
    }

    @Override
    public void accept(IRepositoryVisitor visitor, int options)
    {
        IOSGiInstall install = SigilCore.getInstallManager().getDefaultInstall();

        if (install != null)
        {
            List<ISigilBundle> found = null;

            synchronized (this)
            {
                found = bundles == null ? null : bundles.get(install.getId());
            }

            if (found == null)
            {
                found = new ArrayList<ISigilBundle>();
                IPath source = install.getType().getSourceLocation();

                for (IPath p : install.getType().getDefaultBundleLocations())
                {
                    loadBundle(p, found, source);
                }

                synchronized (this)
                {
                    bundles = new HashMap<String, List<ISigilBundle>>();
                    bundles.put(install.getId(), found);
                }
            }

            for (ISigilBundle b : found)
            {
                if (!visitor.visit(b))
                {
                    break;
                }
            }
        }
    }

    private void loadBundle(IPath p, List<ISigilBundle> bundles, IPath source)
    {
        File f = p.toFile();
        JarFile jar = null;
        try
        {
            jar = new JarFile(f, false);
            ISigilBundle bundle = buildBundle(jar, f);
            if (bundle != null)
            {
                bundle.setLocation(f);
                bundle.setSourcePathLocation(source.toFile());
                // XXX hard coded src location
                bundle.setSourceRootPath("src");
                bundles.add(bundle);
            }
        }
        catch (IOException e)
        {
            BldCore.error("Failed to read jar file " + f, e);
        }
        catch (ModelElementFactoryException e)
        {
            BldCore.error("Failed to build bundle " + f, e);
        }
        catch (RuntimeException e)
        {
            BldCore.error("Failed to build bundle " + f, e);
        }
        finally
        {
            if (jar != null)
            {
                try
                {
                    jar.close();
                }
                catch (IOException e)
                {
                    BldCore.error("Failed to close jar file", e);
                }
            }
        }
    }

    private ISigilBundle buildBundle(JarFile jar, File f) throws IOException
    {
        IBundleModelElement info = ManifestUtil.buildBundleModelElement(jar);

        ISigilBundle bundle = null;

        if (info != null)
        {
            bundle = ModelElementFactory.getInstance().newModelElement(ISigilBundle.class);
            bundle.addChild(info);
            bundle.setLocation(f);
        }

        return bundle;
    }
}
