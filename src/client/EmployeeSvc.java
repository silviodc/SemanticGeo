package client;

import java.io.File;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.util.FileManager;

import server.GazetteerService;
import server.GazetteerServiceImpl;
import server.Locality;
 

@Path("/xmlServices")
public class EmployeeSvc {
	
	private static final double similarity = 0.7;
	private static final String endpoint="http://biomac.icmc.usp.br:8080/swiendpoint/Query";
	 	
     @GET
     @Path("/search/{query}")
     @Produces(MediaType.APPLICATION_XML)
  
   
 	
     
     public HashMap<String,Locality> getStudent( @PathParam("query") String query ) {
    	 GazetteerService servico = new GazetteerServiceImpl();		
 		String ontologyIRI = "files"+File.separator+"Gazetteer_v_1_1.owl";
 		File f = new File(ontologyIRI);
 		System.out.println(f.getAbsolutePath());
 		OntModel m = ModelFactory.createOntologyModel();
 		InputStream in = FileManager.get().open(ontologyIRI);
 		OntModel model = (OntModel) m.read(in,"");		
 		HashMap<String,Locality> list = null;
 		try {
 			list = servico.searchLocalities(query,model,similarity,endpoint);
/* 			Set<String> key = list.keySet();
 			Iterator<String> it = key.iterator();
 			while(it.hasNext()){
 				String chave = it.next();
 				System.out.println(list.get(chave).getScore()+" "+list.get(chave).getLocality()+"  "+list.get(chave).getType()+" "+list.get(chave).getGeometry());
 			}
 			
*/ 		
 			return list;
 			} catch (Exception e) {
 			e.printStackTrace();
 		}
		return list;
 		
     }
}