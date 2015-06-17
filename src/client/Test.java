package client;

import java.io.File;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

import server.GazetteerService;
import server.GazetteerServiceImpl;
import server.Locality;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.util.FileManager;

public class Test {
	
	
	private static final double similarity = 0.7;
	private static final String endpoint="http://biomac.icmc.usp.br:8080/swiendpoint/Query";
	
	
	public static void main(String args[]){
	
		
	}
	
	
	public void testes(){
		GazetteerService servico = new GazetteerServiceImpl();		
		String ontologyIRI = "files"+File.separator+"Gazetteer_v_1_1.owl";
		File f = new File(ontologyIRI);
		System.out.println(f.getAbsolutePath());
		OntModel m = ModelFactory.createOntologyModel();
		InputStream in = FileManager.get().open(ontologyIRI);
		OntModel model = (OntModel) m.read(in,"");		
		
		try {
			HashMap<String,Locality> list = servico.searchLocalities("Reservas proximas a 100 km do municipio de manaus",model,similarity,endpoint);
			Set<String> key = list.keySet();
			Iterator<String> it = key.iterator();
			while(it.hasNext()){
				String chave = it.next();
				System.out.println(list.get(chave).getScore()+" "+list.get(chave).getLocality()+"  "+list.get(chave).getType()+" "+list.get(chave).getGeometry());
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		try {
			HashMap<String,Locality> list = servico.searchLocalities("Reserva adolpho Ducke",model,similarity,endpoint);
			Set<String> key = list.keySet();
			Iterator<String> it = key.iterator();
			while(it.hasNext()){
				String chave = it.next();
				System.out.println(list.get(chave).getScore()+" "+list.get(chave).getLocality()+"  "+list.get(chave).getType()+" "+list.get(chave).getGeometry());
			}
			
		} catch (Exception e) {
			
			e.printStackTrace();
		}
	}
}
