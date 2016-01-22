/*
 *------------------------------------------------------------------------------
 *  Copyright (C) 2015-2016 University of Dundee. All rights reserved.
 *
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along
 *  with this program; if not, write to the Free Software Foundation, Inc.,
 *  51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 *------------------------------------------------------------------------------
 */
package omero.gateway.facility;

import java.io.File;
import java.io.FileInputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import omero.ServerError;
import omero.api.IContainerPrx;
import omero.api.IUpdatePrx;
import omero.cmd.CmdCallbackI;
import omero.api.RawFileStorePrx;
import omero.cmd.Request;
import omero.cmd.Response;
import omero.gateway.Gateway;
import omero.gateway.SecurityContext;
import omero.gateway.exception.DSAccessException;
import omero.gateway.exception.DSOutOfServiceException;
import omero.gateway.model.DataObject;
import omero.gateway.model.DatasetData;
import omero.gateway.model.ImageData;
import omero.gateway.util.PojoMapper;
import omero.gateway.util.Requests;
import omero.model.ChecksumAlgorithm;
import omero.model.ChecksumAlgorithmI;
import omero.model.DatasetAnnotationLink;
import omero.model.DatasetAnnotationLinkI;
import omero.model.DatasetImageLink;
import omero.model.DatasetImageLinkI;
import omero.model.FileAnnotation;
import omero.model.FileAnnotationI;
import omero.model.IObject;
import omero.model.ImageAnnotationLink;
import omero.model.ImageAnnotationLinkI;
import omero.model.OriginalFile;
import omero.model.OriginalFileI;
import omero.model.PlateAnnotationLink;
import omero.model.PlateAnnotationLinkI;
import omero.model.ProjectAnnotationLink;
import omero.model.ProjectAnnotationLinkI;
import omero.model.ProjectDatasetLink;
import omero.model.ProjectDatasetLinkI;
import omero.model.ScreenAnnotationLink;
import omero.model.ScreenAnnotationLinkI;
import omero.model.WellAnnotationLink;
import omero.model.WellAnnotationLinkI;
import omero.model.enums.ChecksumAlgorithmSHA1160;
import omero.sys.Parameters;
import omero.gateway.model.AnnotationData;
import omero.gateway.model.FileAnnotationData;
import omero.gateway.model.PlateData;
import omero.gateway.model.ProjectData;
import omero.gateway.model.ScreenData;
import omero.gateway.model.WellData;
import omero.gateway.model.WellSampleData;

/**
 * A {@link Facility} for saving, deleting and updating data objects
 * 
 * @author Dominik Lindner &nbsp;&nbsp;&nbsp;&nbsp; <a
 *         href="mailto:d.lindner@dundee.ac.uk">d.lindner@dundee.ac.uk</a>
 * @since 5.1
 */

public class DataManagerFacility extends Facility {
    
    /** Reference to the {@link BrowseFacility} */
    private BrowseFacility browse;

    /** Default file upload buffer size */
    private int INC = 262144;
    
    /**
     * Creates a new instance
     * 
     * @param gateway
     *            Reference to the {@link Gateway}
     */
    DataManagerFacility(Gateway gateway) throws ExecutionException {
        super(gateway);
        this.browse = gateway.getFacility(BrowseFacility.class);
    }

    /**
     * Deletes the specified object.
     *
     * @deprecated Use the asynchronous method
     *             {@link #delete(SecurityContext, IObject)} instead
     * @param ctx
     *            The security context.
     * @param object
     *            The object to delete.
     * @return The {@link Response} handle
     * @throws DSOutOfServiceException
     *             If the connection is broken, or not logged in
     * @throws DSAccessException
     *             If an error occurred while trying to retrieve data from OMERO
     *             service.
     */
    public Response deleteObject(SecurityContext ctx, IObject object)
            throws DSOutOfServiceException, DSAccessException {
        return deleteObjects(ctx, Collections.singletonList(object));
    }

    /**
     * Deletes the specified object asynchronously
     * 
     * @param ctx
     *            The security context.
     * @param object
     *            The object to delete.
     * @return The {@link CmdCallbackI}
     * @throws DSOutOfServiceException
     *             If the connection is broken, or not logged in
     * @throws DSAccessException
     *             If an error occurred while trying to retrieve data from OMERO
     *             service.
     */
    public CmdCallbackI delete(SecurityContext ctx, IObject object)
            throws DSOutOfServiceException, DSAccessException {
        return delete(ctx, Collections.singletonList(object));
    }

    /**
     * Deletes the specified objects asynchronously
     *
     * @param ctx
     *            The security context.
     * @param objects
     *            The objects to delete.
     * @return The {@link CmdCallbackI}
     * @throws DSOutOfServiceException
     *             If the connection is broken, or logged in
     * @throws DSAccessException
     *             If an error occurred while trying to retrieve data from OMERO
     *             service.
     */
    public CmdCallbackI delete(SecurityContext ctx, List<IObject> objects)
            throws DSOutOfServiceException, DSAccessException {
        try {
            /*
             * convert the list of objects to lists of IDs by OMERO model class
             * name
             */
            final Map<String, List<Long>> objectIds = new HashMap<String, List<Long>>();
            for (final IObject object : objects) {
                /* determine actual model class name for this object */
                Class<? extends IObject> objectClass = object.getClass();
                while (true) {
                    final Class<?> superclass = objectClass.getSuperclass();
                    if (IObject.class == superclass) {
                        break;
                    } else {
                        objectClass = superclass.asSubclass(IObject.class);
                    }
                }
                final String objectClassName = objectClass.getSimpleName();
                /* then add the object's ID to the list for that class name */
                final Long objectId = object.getId().getValue();
                List<Long> idsThisClass = objectIds.get(objectClassName);
                if (idsThisClass == null) {
                    idsThisClass = new ArrayList<Long>();
                    objectIds.put(objectClassName, idsThisClass);
                }
                idsThisClass.add(objectId);
            }
            /* now delete the objects */
            final Request request = Requests.delete(objectIds);
            return gateway.submit(ctx, request);
        } catch (Throwable t) {
            handleException(this, t, "Cannot delete the object.");
        }
        return null;
    }

    /**
     * Deletes the specified objects.
     *
     * @deprecated Use the asynchronous method
     *             {@link #delete(SecurityContext, List)} instead
     * 
     * @param ctx
     *            The security context.
     * @param objects
     *            The objects to delete.
     * @return The {@link Response} handle
     * @throws DSOutOfServiceException
     *             If the connection is broken, or not logged in
     * @throws DSAccessException
     *             If an error occurred while trying to retrieve data from OMERO
     *             service.
     */
    public Response deleteObjects(SecurityContext ctx, List<IObject> objects)
            throws DSOutOfServiceException, DSAccessException {
        try {
            /*
             * convert the list of objects to lists of IDs by OMERO model class
             * name
             */
            final Map<String, List<Long>> objectIds = new HashMap<String, List<Long>>();
            for (final IObject object : objects) {
                /* determine actual model class name for this object */
                Class<? extends IObject> objectClass = object.getClass();
                while (true) {
                    final Class<?> superclass = objectClass.getSuperclass();
                    if (IObject.class == superclass) {
                        break;
                    } else {
                        objectClass = superclass.asSubclass(IObject.class);
                    }
                }
                final String objectClassName = objectClass.getSimpleName();
                /* then add the object's ID to the list for that class name */
                final Long objectId = object.getId().getValue();
                List<Long> idsThisClass = objectIds.get(objectClassName);
                if (idsThisClass == null) {
                    idsThisClass = new ArrayList<Long>();
                    objectIds.put(objectClassName, idsThisClass);
                }
                idsThisClass.add(objectId);
            }
            /* now delete the objects */
            final Request request = Requests.delete(objectIds);
            return gateway.submit(ctx, request).loop(50, 250);
        } catch (Throwable t) {
            handleException(this, t, "Cannot delete the object.");
        }
        return null;
    }

    /**
     * Updates the specified object.
     *
     * @param ctx
     *            The security context.
     * @param object
     *            The object to update.
     * @param options
     *            Options to update the data.
     * @return The updated object.
     * @throws DSOutOfServiceException
     *             If the connection is broken, or not logged in
     * @throws DSAccessException
     *             If an error occurred while trying to retrieve data from OMERO
     *             service.
     * @see IContainerPrx#updateDataObject(IObject, Parameters)
     */
    public IObject saveAndReturnObject(SecurityContext ctx, IObject object,
            Map options) throws DSOutOfServiceException, DSAccessException {
        try {
            IUpdatePrx service = gateway.getUpdateService(ctx);
            if (options == null)
                return service.saveAndReturnObject(object);
            return service.saveAndReturnObject(object, options);
        } catch (Throwable t) {
            handleException(this, t, "Cannot update the object.");
        }
        return null;
    }

    /**
     * Updates the specified object.
     *
     * @param ctx
     *            The security context.
     * @param object
     *            The object to update.
     * @return The updated object.
     * @throws DSOutOfServiceException
     *             If the connection is broken, or not logged in
     * @throws DSAccessException
     *             If an error occurred while trying to retrieve data from OMERO
     *             service.
     * @see IContainerPrx#updateDataObject(IObject, Parameters)
     */
    public DataObject saveAndReturnObject(SecurityContext ctx, DataObject object)
            throws DSOutOfServiceException, DSAccessException {
        return PojoMapper.asDataObject(saveAndReturnObject(ctx,
                object.asIObject()));
    }

    /**
     * Updates the specified object.
     *
     * @param ctx
     *            The security context.
     * @param object
     *            The object to update.
     * @return The updated object.
     * @throws DSOutOfServiceException
     *             If the connection is broken, or not logged in
     * @throws DSAccessException
     *             If an error occurred while trying to retrieve data from OMERO
     *             service.
     * @see IContainerPrx#updateDataObject(IObject, Parameters)
     */
    public IObject saveAndReturnObject(SecurityContext ctx, IObject object)
            throws DSOutOfServiceException, DSAccessException {
        try {
            IUpdatePrx service = gateway.getUpdateService(ctx);
            IObject result = service.saveAndReturnObject(object);
            return result;
        } catch (Throwable t) {
            handleException(this, t, "Cannot update the object.");
        }
        return null;
    }

    /**
     * Updates the specified object.
     *
     * @param ctx
     *            The security context.
     * @param object
     *            The object to update.
     * @param options
     *            Options to update the data.
     * @param userName
     *            The name of the user to create the data for.
     * @return The updated object.
     * @throws DSOutOfServiceException
     *             If the connection is broken, or not logged in
     * @throws DSAccessException
     *             If an error occurred while trying to retrieve data from OMERO
     *             service.
     * @see IContainerPrx#updateDataObject(IObject, Parameters)
     */
    public IObject saveAndReturnObject(SecurityContext ctx, IObject object,
            Map options, String userName) throws DSOutOfServiceException,
            DSAccessException {
        try {
            IUpdatePrx service = gateway.getUpdateService(ctx, userName);

            if (options == null)
                return service.saveAndReturnObject(object);
            return service.saveAndReturnObject(object, options);
        } catch (Throwable t) {
            handleException(this, t, "Cannot update the object.");
        }
        return null;
    }

    /**
     * Updates the specified object.
     *
     * @param ctx
     *            The security context.
     * @param object
     *            The object to update.
     * @param userName
     *            The name of the user to create the data for.
     * @return The updated object.
     * @throws DSOutOfServiceException
     *             If the connection is broken, or not logged in
     * @throws DSAccessException
     *             If an error occurred while trying to retrieve data from OMERO
     *             service.
     * @see IContainerPrx#updateDataObject(IObject, Parameters)
     */
    public DataObject saveAndReturnObject(SecurityContext ctx,
            DataObject object, String userName) throws DSOutOfServiceException,
            DSAccessException {
        return PojoMapper.asDataObject(saveAndReturnObject(ctx,
                object.asIObject(), userName));
    }

    /**
     * Updates the specified object.
     *
     * @param ctx
     *            The security context.
     * @param object
     *            The object to update.
     * @param userName
     *            The name of the user to create the data for.
     * @return The updated object.
     * @throws DSOutOfServiceException
     *             If the connection is broken, or not logged in
     * @throws DSAccessException
     *             If an error occurred while trying to retrieve data from OMERO
     *             service.
     * @see IContainerPrx#updateDataObject(IObject, Parameters)
     */
    public IObject saveAndReturnObject(SecurityContext ctx, IObject object,
            String userName) throws DSOutOfServiceException, DSAccessException {
        try {
            IUpdatePrx service = gateway.getUpdateService(ctx, userName);
            IObject result = service.saveAndReturnObject(object);
            return result;
        } catch (Throwable t) {
            handleException(this, t, "Cannot update the object.");
        }
        return null;
    }

    /**
     * Updates the specified object.
     *
     * @param ctx
     *            The security context.
     * @param objects
     *            The objects to update.
     * @param options
     *            Options to update the data.
     * @param userName
     *            The username
     * @return The updated object.
     * @throws DSOutOfServiceException
     *             If the connection is broken, or not logged in
     * @throws DSAccessException
     *             If an error occurred while trying to retrieve data from OMERO
     *             service.
     * @see IContainerPrx#updateDataObject(IObject, Parameters)
     */
    public List<IObject> saveAndReturnObject(SecurityContext ctx,
            List<IObject> objects, Map options, String userName)
            throws DSOutOfServiceException, DSAccessException {
        try {
            IUpdatePrx service = gateway.getUpdateService(ctx, userName);
            return service.saveAndReturnArray(objects);
        } catch (Throwable t) {
            handleException(this, t, "Cannot update the object.");
        }
        return new ArrayList<IObject>();
    }

    /**
     * Updates the specified object.
     *
     * @param ctx
     *            The security context.
     * @param object
     *            The object to update.
     * @param options
     *            Options to update the data.
     * @return The updated object.
     * @throws DSOutOfServiceException
     *             If the connection is broken, or not logged in
     * @throws DSAccessException
     *             If an error occurred while trying to retrieve data from OMERO
     *             service.
     * @see IContainerPrx#updateDataObject(IObject, Parameters)
     */
    public IObject updateObject(SecurityContext ctx, IObject object,
            Parameters options) throws DSOutOfServiceException,
            DSAccessException {
        try {
            IContainerPrx service = gateway.getPojosService(ctx);
            IObject r = service.updateDataObject(object, options);
            return browse.findIObject(ctx, r);
        } catch (Throwable t) {
            handleException(this, t, "Cannot update the object.");
        }
        return null;
    }

    /**
     * Updates the specified <code>IObject</code>s and returned the updated
     * <code>IObject</code>s.
     *
     * @param ctx
     *            The security context.
     * @param objects
     *            The array of objects to update.
     * @param options
     *            Options to update the data.
     * @return See above.
     * @throws DSOutOfServiceException
     *             If the connection is broken, or not logged in
     * @throws DSAccessException
     *             If an error occurred while trying to retrieve data from OMERO
     *             service.
     * @see IContainerPrx#updateDataObjects(List, Parameters)
     */
    public List<IObject> updateObjects(SecurityContext ctx,
            List<IObject> objects, Parameters options)
            throws DSOutOfServiceException, DSAccessException {
        try {
            IContainerPrx service = gateway.getPojosService(ctx);
            List<IObject> l = service.updateDataObjects(objects, options);
            if (l == null)
                return l;
            Iterator<IObject> i = l.iterator();
            List<IObject> r = new ArrayList<IObject>(l.size());
            IObject io;
            while (i.hasNext()) {
                io = browse.findIObject(ctx, i.next());
                if (io != null)
                    r.add(io);
            }
            return r;
        } catch (Throwable t) {
            handleException(this, t, "Cannot update the object.");
        }
        return new ArrayList<IObject>();
    }

    /**
     * Adds the {@link ImageData} to the given {@link DatasetData}
     * 
     * @param ctx
     *            The security context.
     * @param image
     *            The image to add to the dataset
     * @param ds
     *            The dataset to add the image to
     * @throws DSOutOfServiceException
     *             If the connection is broken, or not logged in
     * @throws DSAccessException
     *             If an error occurred while trying to retrieve data from OMERO
     *             service.
     */
    public void addImageToDataset(SecurityContext ctx, ImageData image,
            DatasetData ds) throws DSOutOfServiceException, DSAccessException {
        List<ImageData> l = new ArrayList<ImageData>(1);
        l.add(image);
        addImagesToDataset(ctx, l, ds);
    }

    /**
     * Adds the {@link ImageData}s to the given {@link DatasetData}
     * 
     * @param ctx
     *            The security context.
     * @param images
     *            The images to add to the dataset
     * @param ds
     *            The dataset to add the images to
     * @throws DSOutOfServiceException
     *             If the connection is broken, or not logged in
     * @throws DSAccessException
     *             If an error occurred while trying to retrieve data from OMERO
     *             service.
     */
    public void addImagesToDataset(SecurityContext ctx,
            Collection<ImageData> images, DatasetData ds)
            throws DSOutOfServiceException, DSAccessException {
        List<IObject> links = new ArrayList<IObject>();
        for (ImageData img : images) {
            DatasetImageLink l = new DatasetImageLinkI();
            l.setParent(ds.asDataset());
            l.setChild(img.asImage());
            links.add(l);
        }
        updateObjects(ctx, links, null);
    }

    /**
     * Creates the {@link DatasetData} on the server and attaches it
     * to the {@link ProjectData} (if not <code>null</code>) (if the 
     * project doesn't exist on the server yet, it will be created, too)
     * @param ctx The {@link SecurityContext}
     * @param dataset The {@link DatasetData}
     * @param project The {@link ProjectData}
     * @return The {@link DatasetData}
     * @throws DSOutOfServiceException
     *             If the connection is broken, or not logged in
     * @throws DSAccessException
     *             If an error occurred while trying to retrieve data from OMERO
     *             service.
     */
    public DatasetData createDataset(SecurityContext ctx, DatasetData dataset,
            ProjectData project) throws DSOutOfServiceException,
            DSAccessException {
        if (project != null) {
            ProjectDatasetLink link = new ProjectDatasetLinkI();
            link = new ProjectDatasetLinkI();
            link.setChild(dataset.asDataset());
            link.setParent(project.asProject());
            link = (ProjectDatasetLink) saveAndReturnObject(ctx, link);
            return new DatasetData(link.getChild());
        } else {
            return (DatasetData) saveAndReturnObject(ctx, dataset);
        }
    }
    
    /**
     * Uploads and attaches a file to the provided {@link DataObject} (if
     * provided)
     * 
     * @param ctx
     *            {@link SecurityContext}
     * @param file
     *            The {@link File} to upload/attach
     * @param mimetype
     *            The mime type of the file (can be <code>null</code>)
     * @param description
     *            The description (can be <code>null</code>)
     * @param namespace
     *            The namespace (can be <code>null</code>)
     * @param target
     *            The {@link DataObject} to attach the file to (can be
     *            <code>null</code>)
     * @param callback
     *            If no {@link Callback} is provided, the method will block
     *            until the task is finished, otherwise the upload runs
     *            asynchronously and the {@link Callback} handle gets notified
     *            about the outcome
     * @return The {@link FileAnnotationData} if no {@link Callback} handle was
     *         provided, <code>null</code> otherwise
     */
    public FileAnnotationData attachFile(final SecurityContext ctx,
            final File file, String mimetype, final String description,
            final String namespace, final DataObject target, Callback callback) {
        final String name = file.getName();
        String absolutePath = file.getAbsolutePath();
        final String path = absolutePath.substring(0, absolutePath.length()
                - name.length());

        final String mime;
        if (mimetype == null)
            mime = "application/octet-stream";
        else
            mime = mimetype;

        final Callback cb = callback != null ? callback : new Callback();

        Runnable r = new Runnable() {
            @Override
            public void run() {
                RawFileStorePrx rawFileStore = null;
                try {
                    OriginalFile originalFile = new OriginalFileI();
                    originalFile.setName(omero.rtypes.rstring(name));
                    originalFile.setPath(omero.rtypes.rstring(path));
                    originalFile.setSize(omero.rtypes.rlong(file.length()));
                    final ChecksumAlgorithm checksumAlgorithm = new ChecksumAlgorithmI();
                    checksumAlgorithm.setValue(omero.rtypes
                            .rstring(ChecksumAlgorithmSHA1160.value));
                    originalFile.setHasher(checksumAlgorithm);
                    originalFile.setMimetype(omero.rtypes.rstring(mime));
                    originalFile = (OriginalFile) saveAndReturnObject(ctx,
                            originalFile);

                    rawFileStore = gateway.getRawFileService(ctx);
                    rawFileStore.setFileId(originalFile.getId().getValue());
                    FileInputStream stream = new FileInputStream(file);
                    long pos = 0;
                    int rlen;
                    byte[] buf = new byte[INC];
                    ByteBuffer bbuf;
                    while (((rlen = stream.read(buf)) > 0) && !cb.isCancelled()) {
                        rawFileStore.write(buf, pos, rlen);
                        pos += rlen;
                        bbuf = ByteBuffer.wrap(buf);
                        bbuf.limit(rlen);
                    }
                    stream.close();

                    if (cb.isCancelled()) {
                        try {
                            rawFileStore.close();
                        } catch (ServerError e) {
                        }
                        cb.setResult(null);
                        return;
                    }

                    originalFile = rawFileStore.save();

                    FileAnnotation fa = new FileAnnotationI();
                    fa.setFile(originalFile);
                    if (description != null)
                        fa.setDescription(omero.rtypes.rstring(description));
                    fa.setNs(omero.rtypes.rstring(namespace));
                    fa = (FileAnnotation) saveAndReturnObject(ctx, fa);

                    if (target != null)
                        cb.setResult(attachAnnotation(ctx,
                                new FileAnnotationData(fa), target));
                    else
                        cb.setResult(new FileAnnotationData(fa));
                } catch (Throwable t) {
                    cb.setException(t);
                } finally {
                    if (rawFileStore != null) {
                        try {
                            rawFileStore.close();
                        } catch (ServerError e) {
                        }
                    }
                }
            }
        };

        if (callback != null) {
            (new Thread(r)).start();
            return null;
        }
        
        r.run();
        return (FileAnnotationData) cb.result;
    }
    
    /**
     * Create/attach an {@link AnnotationData} to a given {@link DataObject}
     * 
     * @param ctx
     *            The {@link SecurityContext}
     * @param annotation
     *            The {@link AnnotationData}
     * @param target
     *            The {@link DataObject} to attach to
     * @return The {@link AnnotationData}
     * @throws DSOutOfServiceException
     *             If the connection is broken, or not logged in
     * @throws DSAccessException
     *             If an error occurred while trying to retrieve data from OMERO
     *             service.
     */
    public <T extends AnnotationData> T attachAnnotation(SecurityContext ctx,
            T annotation, DataObject target) throws DSOutOfServiceException,
            DSAccessException {
        if (target != null) {
            if (target instanceof ProjectData) {
                ProjectData project = browse.findObject(ctx, ProjectData.class,
                        target.getId());
                ProjectAnnotationLink link = new ProjectAnnotationLinkI();
                link.setChild(annotation.asAnnotation());
                link.setParent(project.asProject());
                link = (ProjectAnnotationLink) saveAndReturnObject(ctx, link);
                return (T) PojoMapper.asDataObject(link.getChild());
            }
            if (target instanceof DatasetData) {
                DatasetData ds = browse.findObject(ctx, DatasetData.class,
                        target.getId());
                DatasetAnnotationLink link = new DatasetAnnotationLinkI();
                link.setChild(annotation.asAnnotation());
                link.setParent(ds.asDataset());
                link = (DatasetAnnotationLink) saveAndReturnObject(ctx, link);
                return (T) PojoMapper.asDataObject(link.getChild());
            }
            if (target instanceof ScreenData) {
                ScreenData s = browse.findObject(ctx, ScreenData.class,
                        target.getId());
                ScreenAnnotationLink link = new ScreenAnnotationLinkI();
                link.setChild(annotation.asAnnotation());
                link.setParent(s.asScreen());
                link = (ScreenAnnotationLink) saveAndReturnObject(ctx, link);
                return (T) PojoMapper.asDataObject(link.getChild());
            }
            if (target instanceof PlateData) {
                PlateData p = browse.findObject(ctx, PlateData.class,
                        target.getId());
                PlateAnnotationLink link = new PlateAnnotationLinkI();
                link.setChild(annotation.asAnnotation());
                link.setParent(p.asPlate());
                link = (PlateAnnotationLink) saveAndReturnObject(ctx, link);
                return (T) PojoMapper.asDataObject(link.getChild());
            }
            if (target instanceof WellData) {
                WellData w = browse.findObject(ctx, WellData.class,
                        target.getId());
                WellAnnotationLink link = new WellAnnotationLinkI();
                link.setChild(annotation.asAnnotation());
                link.setParent(w.asWell());
                link = (WellAnnotationLink) saveAndReturnObject(ctx, link);
                return (T) PojoMapper.asDataObject(link.getChild());
            }
            if (target instanceof ImageData) {
                ImageData i = browse.findObject(ctx, ImageData.class,
                        target.getId());
                ImageAnnotationLink link = new ImageAnnotationLinkI();
                link.setChild(annotation.asAnnotation());
                link.setParent(i.asImage());
                link = (ImageAnnotationLink) saveAndReturnObject(ctx, link);
                return (T) PojoMapper.asDataObject(link.getChild());
            }
            if (target instanceof WellSampleData) {
                WellSampleData w = browse.findObject(ctx, WellSampleData.class,
                        target.getId());
                ImageData i = browse.findObject(ctx, ImageData.class, w
                        .getImage().getId());
                ImageAnnotationLink link = new ImageAnnotationLinkI();
                link.setChild(annotation.asAnnotation());
                link.setParent(i.asImage());
                link = (ImageAnnotationLink) saveAndReturnObject(ctx, link);
                return (T) PojoMapper.asDataObject(link.getChild());
            }
        } else {
            return (T) saveAndReturnObject(ctx, annotation);
        }
        return null;
    }
    
}
