package server;

import java.util.HashMap;
import java.util.List;

import com.hp.hpl.jena.ontology.OntModel;


public interface GazetteerService {

	public List<Locality> findPlacesWithOutCoord(String endpoint);
	public HashMap<String,Locality> searchLocalities(String search, OntModel model, double similarity, String endpoint) throws Exception;
	public Integer insertLocality(Locality locality,String user, String password,String endpoint);
	public void agreeLinkedData(Locality locality,String user, String password,String endpoint);
	public List<Float[]> getPolygons(String endpoint);
	public List<String> getOntTypes(String endpoint);
	public String[] infoServer(String endpoint);
}
