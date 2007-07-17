/*
 * org.openmicroscopy.shoola.agents.measurement.util.AnalysisStatsWrapper 
 *
  *------------------------------------------------------------------------------
 *  Copyright (C) 2006-2007 University of Dundee. All rights reserved.
 *
 *
 * 	This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *  
 *  You should have received a copy of the GNU General Public License along
 *  with this program; if not, write to the Free Software Foundation, Inc.,
 *  51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 *------------------------------------------------------------------------------
 */
package org.openmicroscopy.shoola.agents.measurement.util;

//Java imports
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

//Third-party libraries

//Application-internal dependencies
import org.openmicroscopy.shoola.env.rnd.roi.ROIShapeStats;
import org.openmicroscopy.shoola.util.math.geom2D.PlanePoint2D;
import org.openmicroscopy.shoola.util.roi.model.ROIShape;

/** 
 * 
 *
 * @author  Jean-Marie Burel &nbsp;&nbsp;&nbsp;&nbsp;
 * 	<a href="mailto:j.burel@dundee.ac.uk">j.burel@dundee.ac.uk</a>
 * @author	Donald MacDonald &nbsp;&nbsp;&nbsp;&nbsp;
 * 	<a href="mailto:donald@lifesci.dundee.ac.uk">donald@lifesci.dundee.ac.uk</a>
 * @version 3.0
 * <small>
 * (<b>Internal version:</b> $Revision: $Date: $)
 * </small>
 * @since OME3.0
 */
public class AnalysisStatsWrapper
{	
	public enum StatsType 
	{
		MIN,
		MAX, 
		MEAN,
		STDDEV,
		PIXELDATA,
	};

	
	public static Map<StatsType, Map> convertStats(Map shapeStats)
	{
		if(shapeStats==null || shapeStats.size()==0)
			return null;
		ROIShapeStats 		stats;
		int numChannels = shapeStats.size();
		Map<Integer, Double>   channelMin = new TreeMap<Integer, Double>();
		Map<Integer, Double>   channelMax = new TreeMap<Integer, Double>();
		Map<Integer, Double>   channelMean = new TreeMap<Integer, Double>();
		Map<Integer, Double>   channelStdDev = new TreeMap<Integer, Double>();
		Map<Integer, double[]>	channelData = new TreeMap<Integer, double[]>();
		Iterator<Double> 	pixelIterator;
		Map<PlanePoint2D,Double> pixels;
		double[] pixelData;
		int cnt;
		int channel;
		Iterator channelIterator = shapeStats.keySet().iterator();
		while (channelIterator.hasNext())
		{
			channel = (Integer) channelIterator.next();
			stats = (ROIShapeStats) shapeStats.get(channel);
			channelMin.put(channel, stats.getMin());
			channelMax.put(channel, stats.getMax());
			channelMean.put(channel, stats.getMean());
			channelStdDev.put(channel, stats.getStandardDeviation());
			pixels = stats.getPixelsValue();
			
			pixelIterator = pixels.values().iterator();
			pixelData = new double[pixels.size()];
			cnt = 0;
			while (pixelIterator.hasNext())
			{
				double dataPt = pixelIterator.next();
				pixelData[cnt] = dataPt;
				cnt++;
			}
			
			channelData.put(channel, pixelData);
		}
		Map<StatsType, Map> 
			statsMap = new HashMap<StatsType, Map>(StatsType.values().length);
		statsMap.put(StatsType.MIN, channelMin);
		statsMap.put(StatsType.MAX, channelMax);
		statsMap.put(StatsType.MEAN, channelMean);
		statsMap.put(StatsType.STDDEV, channelStdDev);
		statsMap.put(StatsType.PIXELDATA, channelData);
		return statsMap;
	}
	
}


