/* ome.ro.ejb.AbstractBean
 *
 *------------------------------------------------------------------------------
 *
 *  Copyright (C) 2005 Open Microscopy Environment
 *      Massachusetts Institute of Technology,
 *      National Institutes of Health,
 *      University of Dundee
 *
 *
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation; either
 *    version 2.1 of the License, or (at your option) any later version.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 *
 *    You should have received a copy of the GNU Lesser General Public
 *    License along with this library; if not, write to the Free Software
 *    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 *------------------------------------------------------------------------------
 */

package ome.ro.ejb;

//Java imports

//Third-party imports
import javax.annotation.Resource;
import javax.ejb.SessionContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

//Application-internal dependencies
import ome.system.OmeroContext;

public class AbstractBean 
{
    
    private static Log log = LogFactory.getLog(AbstractBean.class);

    protected OmeroContext  applicationContext = OmeroContext.getManagedServerContext();

    // java:comp.ejb3/EJBContext
    protected @Resource SessionContext sessionContext; 
    
    public AbstractBean()
    {
        log.debug("Creating:\n"+getLogString());
    }
    
    public void destroy()
    {
        log.debug("Destroying:\n"+getLogString());
    }
    
    protected String getLogString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("Bean ");
        sb.append(this);
        sb.append("\n with Context ");
        sb.append(applicationContext);
        return sb.toString();
    }
    
}
