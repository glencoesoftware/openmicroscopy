/*
 * org.openmicroscopy.shoola.env.data.map.DatasetDataMapper
 *
 *------------------------------------------------------------------------------
 *
 *  Copyright (C) 2004 Open Microscopy Environment
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

package org.openmicroscopy.shoola.env.data.map;

//Java imports
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

//Third-party libraries

//Application-internal dependencies
import org.openmicroscopy.ds.Criteria;
import org.openmicroscopy.ds.dto.Dataset;
import org.openmicroscopy.ds.dto.Image;
import org.openmicroscopy.ds.st.Experimenter;
import org.openmicroscopy.ds.st.Group;
import org.openmicroscopy.ds.st.ImageAnnotation;
import org.openmicroscopy.ds.st.Pixels;
import org.openmicroscopy.shoola.env.data.model.DatasetData;
import org.openmicroscopy.shoola.env.data.model.DatasetSummary;
import org.openmicroscopy.shoola.env.data.model.ImageSummary;
import org.openmicroscopy.shoola.env.data.model.PixelsDescription;

/** 
 * 
 *
 * @author  Jean-Marie Burel &nbsp;&nbsp;&nbsp;&nbsp;
 * 				<a href="mailto:j.burel@dundee.ac.uk">j.burel@dundee.ac.uk</a>
 * @author  <br>Andrea Falconi &nbsp;&nbsp;&nbsp;&nbsp;
 * 				<a href="mailto:a.falconi@dundee.ac.uk">
 * 					a.falconi@dundee.ac.uk</a>
 * @version 2.2 
 * <small>
 * (<b>Internal version:</b> $Revision$ $Date$)
 * </small>
 * @since OME2.2
 */
public class DatasetMapper
{
	
	/** 
	 * Create the criteria by which the object graph is pulled out.
	 * Criteria built for updateDataset.
	 * 
	 * @param datasetID	specified dataset to retrieve.
	 */
	public static Criteria buildUpdateCriteria(int datasetID)
	{
		Criteria c = new Criteria();
		c.addWantedField("id");
		c.addWantedField("name");
		c.addWantedField("description");
		c.addFilter("id", new Integer(datasetID));
		return c;
	}
	
	/** 
	 * Create the criteria by which the object graph is pulled out.
	 * Criteria built for retrieveUserDatasets.
	 * 
	 * @param userID	user ID.
	 */
	public static Criteria buildUserDatasetsCriteria(int userID)
	{
		Criteria criteria = new Criteria();
		
		//Specify which fields we want for the dataset.
		criteria.addWantedField("id");
		criteria.addWantedField("name");
		
		criteria.addFilter("owner_id", new Integer(userID));
		criteria.addFilter("name", "NOT LIKE", "ImportSet");
		criteria.addOrderBy("name");
        
		return criteria;
	}
	
	/** 
	 * Create the criteria by which the object graph - including images
	 *  - is pulled out
	 * Criteria built for fullRetrieveUserDatasets.
	 * 
	 * @param userID	user ID.
	 */
	public static Criteria buildFullUserDatasetsCriteria(int userID)
	{
		Criteria criteria = new Criteria();
		
		//Specify which fields we want for the dataset.
		criteria.addWantedField("id");
		criteria.addWantedField("name");
		criteria.addWantedField("description");
		criteria.addWantedField("owner");
		criteria.addWantedField("images");

		//Specify which fields we want for the owner.
		criteria.addWantedField("owner", "id");
		criteria.addWantedField("owner", "FirstName");
		criteria.addWantedField("owner", "LastName");
		criteria.addWantedField("owner", "Email");
		criteria.addWantedField("owner", "Institution");
		criteria.addWantedField("owner", "Group");

		//Specify which fields we want for the owner's group.
		criteria.addWantedField("owner.Group", "id");
		criteria.addWantedField("owner.Group", "Name");

		//Specify which fields we want for the images.
		criteria.addWantedField("images", "id");
		criteria.addWantedField("images", "name");
		criteria.addWantedField("images", "default_pixels");
		
		//Specify which fields we want for the pixels.
		criteria.addWantedField("images.default_pixels", "id"); 
		criteria.addWantedField("images.default_pixels", "ImageServerID"); 
		criteria.addWantedField("images.default_pixels", "Repository");
		criteria.addWantedField("images.default_pixels.Repository",
								"ImageServerURL");		

		criteria.addFilter("owner_id", new Integer(userID));
		criteria.addFilter("name", "NOT LIKE", "ImportSet");
		criteria.addOrderBy("name");
		return criteria;
	}
    
	/**
	 * Create the criteria by which the object graph is pulled out.
	 * Criteria built for retrieveImages.
	 * 
	 * @return 
	 */
	public static Criteria buildImagesCriteria(List ids)
	{
		Criteria criteria = new Criteria();
		
		//Specify which fields we want for the images.
		criteria.addWantedField("images");
		criteria.addWantedField("images", "id");
		criteria.addWantedField("images", "name");
        criteria.addWantedField("images", "created");
		criteria.addWantedField("images", "default_pixels");
		
		//Specify which fields we want for the pixels.
		criteria.addWantedField("images.default_pixels", "id");
		criteria.addWantedField("images.default_pixels", "ImageServerID");
        criteria.addWantedField("images.default_pixels", "Repository");
        criteria.addWantedField("images.default_pixels.Repository",
								"ImageServerURL");
        criteria.addFilter("id", "IN", ids);
		//criteria.addFilter("id", new Integer(id));
        criteria.addOrderBy("id");
		
		return criteria;
	}
	
	/** 
	 * Create the criteria by which the object graph is pulled out.
	 * Criteria built for retrieveDataset.
	 * 
	 */
	public static Criteria buildDatasetCriteria(int id)
	{
		Criteria criteria = new Criteria();

		//Specify which fields we want for the dataset.
		criteria.addWantedField("id");
		criteria.addWantedField("name");
		criteria.addWantedField("description");
		criteria.addWantedField("owner");
		criteria.addWantedField("images"); 

		//Specify which fields we want for the owner.
		criteria.addWantedField("owner", "id");
		criteria.addWantedField("owner", "FirstName");
		criteria.addWantedField("owner", "LastName");
		criteria.addWantedField("owner", "Email");
		criteria.addWantedField("owner", "Institution");
		criteria.addWantedField("owner", "Group");

		//Specify which fields we want for the owner's group.
		criteria.addWantedField("owner.Group", "id");
		criteria.addWantedField("owner.Group", "Name");

		//Specify which fields we want for the images.
		criteria.addWantedField("images", "id");
		criteria.addWantedField("images", "name");
		criteria.addWantedField("images", "default_pixels");
		
		//Specify which fields we want for the pixels.
		criteria.addWantedField("images.default_pixels", "id");
		criteria.addWantedField("images.default_pixels", "ImageServerID");
        criteria.addWantedField("images.default_pixels", "Repository");
        criteria.addWantedField("images.default_pixels.Repository",
								"ImageServerURL");
        criteria.addOrderBy("id");
		
		criteria.addFilter("id", new Integer(id));
		
		return criteria;
	}

	/** 
     * Fill in the dataset data object. 
	 * 
	 * @param dataset	OMEDS dataset object.
	 * @param empty		dataset data to fill up.
	 * 
	 */
	public static void fillDataset(Dataset dataset, DatasetData empty)
	{
		//Fill up the DataObject with the data coming from Project.
		empty.setID(dataset.getID());
		empty.setName(dataset.getName());
		empty.setDescription(dataset.getDescription());
				
		//Fill up the DataObject with data coming from Experimenter.
		Experimenter owner = dataset.getOwner();
		empty.setOwnerID(owner.getID());
		empty.setOwnerFirstName(owner.getFirstName());
		empty.setOwnerLastName(owner.getLastName());
		empty.setOwnerEmail(owner.getEmail());
		empty.setOwnerInstitution(owner.getInstitution());
		
		//Fill up the DataObject with data coming from Group.
		Group group = owner.getGroup();
		empty.setOwnerGroupID(group.getID());
		empty.setOwnerGroupName(group.getName());
		
		//Create the image summary list.
		List images = new ArrayList();
		Iterator i = dataset.getImages().iterator();
		Image img;
		while (i.hasNext()) {
			img = (Image) i.next();
			images.add(new ImageSummary(img.getID(), img.getName(), 
						fillListPixelsID(img),
                        fillDefaultPixels(img.getDefaultPixels())));
		}
		empty.setImages(images);	
	}


	/** Fill in the dataset data object. 
	 * 
	 * @param dataset	OMEDS dataset object.
	 * @param empty		dataset data to fill up.
	 * 
	 */
	public static void fillDataset(Dataset dataset, DatasetData empty,
			                    ImageSummary iProto)
	{
		//Fill up the DataObject with the data coming from Project.
		empty.setID(dataset.getID());
		empty.setName(dataset.getName());
		empty.setDescription(dataset.getDescription());
				
		//Fill up the DataObject with data coming from Experimenter.
		Experimenter owner = dataset.getOwner();
		empty.setOwnerID(owner.getID());
		empty.setOwnerFirstName(owner.getFirstName());
		empty.setOwnerLastName(owner.getLastName());
		empty.setOwnerEmail(owner.getEmail());
		empty.setOwnerInstitution(owner.getInstitution());
		
		//Fill up the DataObject with data coming from Group.
		Group group = owner.getGroup();
		empty.setOwnerGroupID(group.getID());
		empty.setOwnerGroupName(group.getName());
		
		//Create the image summary list.
		List images = new ArrayList();
		List  dsImages = dataset.getImages();
		
		if (dsImages != null && dsImages.size() > 0) {
			Iterator i = dsImages.iterator();
			Image img;
			while (i.hasNext()) {
				img = (Image) i.next();
				ImageSummary is = (ImageSummary) iProto.makeNew();
				is.setID(img.getID());
				is.setName(img.getName());
				is.setPixelsIDs(fillListPixelsID(img));
				is.setDefaultPixels(fillDefaultPixels(img.getDefaultPixels()));
				images.add(is);
			}
			empty.setImages(images);
		}
	}
    
    /** Build a list of imageID. */
    public static List prepareListImagesID(Dataset dataset)
    {
        List ids = new ArrayList();
        List imgs = dataset.getImages();
        if (imgs != null && imgs.size() != 0) {
            Iterator j = imgs.iterator();
            Image image;
            while (j.hasNext()) {
                image = (Image) j.next();
                ids.add(new Integer(image.getID()));
            }
        }
        return ids;
    }
    
    public static List fillImagesInUserDatasets(List datasets, 
                    ImageSummary iProto)
    {
        List images = new ArrayList();
        HashMap map = new HashMap();
        Iterator d = datasets.iterator();
        Iterator i;
        Image image;
        ImageSummary is;
        Integer imageID;
        while (d.hasNext()) {
            i = ((Dataset) d.next()).getImages().iterator();
            while (i.hasNext()) {
                image = (Image) i.next();
                imageID = new Integer(image.getID());
                is = (ImageSummary) map.get(imageID);
                if (is == null) {
                    //Make a new DataObject and fill it up.
                    is = (ImageSummary) iProto.makeNew();
                    is.setID(image.getID());
                    is.setName(image.getName());
                    is.setDate(
                       PrimitiveTypesMapper.getTimestamp(image.getCreated()));
                    is.setPixelsIDs(fillListPixelsID(image));
                    is.setDefaultPixels(fillDefaultPixels(
                                image.getDefaultPixels()));
                    //Add the image summary object to the list.
                    images.add(is);
                    map.put(imageID, is);
                }
            }
        }
        return images;
    }
    
	/**
	 * Creates the image summary list.
	 * 
	 * @param dataset	OMEDS dataset object.
	 * @param iProto	DataObject to fill up.
	 * @return list of image summary objects.
	 */
	public static List fillListImages(Dataset dataset, ImageSummary iProto)
	{
		List datasets = new ArrayList();
        datasets.add(dataset);
        return fillImagesInUserDatasets(datasets, iProto);
	}
	
    /**
     * Creates the image summary list.
     * 
     * @param dataset   OMEDS dataset object.
     * @param iProto    DataObject to fill up.
     * @return list of image summary objects.
     */
    public static List fillListAnnotatedImages(Dataset dataset, 
                                         ImageSummary iProto, List annotations, 
                                         List images)
    {
        Iterator i = dataset.getImages().iterator();
        Image image;
        ImageSummary is;
        int id;
        Map ids = AnnotationMapper.reverseListImageAnnotations(annotations);
        while (i.hasNext()) {
            image = (Image) i.next();
            //Make a new DataObject and fill it up.
            is = (ImageSummary) iProto.makeNew();
            id = image.getID();
            is.setAnnotation(AnnotationMapper.fillImageAnnotation(
                    (ImageAnnotation) ids.get(new Integer(id))));
            is.setID(id);
            is.setName(image.getName());
            is.setDate(PrimitiveTypesMapper.getTimestamp(image.getCreated()));
            is.setPixelsIDs(fillListPixelsID(image));
            is.setDefaultPixels(fillDefaultPixels(image.getDefaultPixels()));
            //Add the image summary object to the list.
            images.add(is);
        }
        return images;
    }
    
	/**
	 * Create a list of dataset summary object.
	 *
	 * @param datasets	list of datasets objects.
	 * @param dProto	dataObject to model.
	 * @return See above.
	 */
	public static List fillUserDatasets(List datasets, DatasetSummary dProto)
	{
		List datasetsList = new ArrayList();  //The returned summary list.
		Iterator i = datasets.iterator();
		DatasetSummary ds;
		Dataset d;
		//For each d in datasets...
		while (i.hasNext()) {
			d = (Dataset) i.next();
			//Make a new DataObject and fill it up.
			ds = (DatasetSummary) dProto.makeNew();
			ds.setID(d.getID());
			ds.setName(d.getName());
			//Add the dataset  summary object to the list.
			datasetsList.add(ds);
		}
		return datasetsList;
	}

	/**
	 * Create a list of dataset summary objects, including images.
	 *
	 * @param datasets	list of datasets objects.
	 * @param dProto	dataObject to model.
	 * @return See above.
	 */
	public static List fillFullUserDatasets(List datasets, DatasetData dProto,
                                            ImageSummary iProto)
	{
		List datasetsList = new ArrayList();  //The returned summary list.
		Iterator i = datasets.iterator();
		DatasetData ds;
		Dataset d;
		//For each d in datasets...
		while (i.hasNext()) {
			d = (Dataset) i.next();
			//Make a new DataObject and fill it up.
			ds = (DatasetData) dProto.makeNew();
			fillDataset(d, ds, iProto);
			datasetsList.add(ds);
		}
		return datasetsList;
	}
	
	//	TODO: will be modified as soon as we have a better approach.
	private static int[] fillListPixelsID(Image image)
	{
		int[] ids = new int[1];
	  	Pixels px = image.getDefaultPixels();
		ids[0] = px.getID();
	  	return ids;
	}
    
    private static PixelsDescription fillDefaultPixels(Pixels px)
    {
        PixelsDescription pxd = new PixelsDescription();
        pxd.setID(px.getID());
        if (px.getImageServerID() != null)
            pxd.setImageServerID(px.getImageServerID().longValue());
        if (px.getRepository().getImageServerURL() != null)
            pxd.setImageServerUrl(px.getRepository().getImageServerURL());
        pxd.setPixels(px);
        return pxd;
    }
	
}
