package server;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.bbn.openmap.geo.Geo;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntProperty;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.sparql.engine.http.QueryEngineHTTP;
import com.hp.hpl.jena.util.FileManager;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;

public class GazetteerServiceImpl  implements GazetteerService{

	/**
	 * Autor Silvio
	 */

	private static final boolean isRead = false;

	private String URL_endpoint = "http://biomac.icmc.usp.br:8080/swiendpoint/Query";
	private double treshold=0.7;

	private static int consulta=0;

	private static HashMap<Integer,Locality> result;
	private static List<Locality> resultGeo;
	private static List<String> classes;
	private static String [] valueCoord;
	private static ArrayList<String> stop_words = new ArrayList<String>();
	private static OntModel model;
	private final int listShow=5;
	private static int count =0;
	private static HashMap<String,OntClass> ontClasses = new HashMap<String,OntClass>();
	private static HashMap<String,OntProperty> ontPropertiess = new HashMap<String,OntProperty>();

	@Override
	public List<Locality> findPlacesWithOutCoord(String endpoint) {
		SPRQLQuery spql = new SPRQLQuery();
		List<Locality> lista = new ArrayList<Locality>();

		if(!spql.askService())
			return lista;
		consulta+=100;
		if(result == null)
			result = spql.makeSPARQLQuery(endpoint,consulta);
		
		for(int i=0; i< result.size(); i++){
			lista.add(result.get(count));
			count++;
			if(i>listShow || i==result.size())
				break;			
		}

		return lista;
	}

	private void loadProperties(){
		ExtendedIterator<OntProperty> iter = model.listAllOntProperties();
		while (iter.hasNext()) {
			OntProperty thisClass = (OntProperty) iter.next();
			ExtendedIterator<?> label = thisClass.listLabels(null);
			while (label.hasNext()) {
				RDFNode thisLabel = (RDFNode) label.next();
				if(thisLabel.isLiteral()){
					String labl = thisLabel.toString().split("http")[0].replaceAll("@en", "").replaceAll("@pt", "").toLowerCase();
					labl = Normalizer.normalize(labl, Normalizer.Form.NFD);  
					labl = labl.replaceAll("[^\\p{ASCII}]", "");
					labl = labl.replaceAll("^^", "");
					if(labl.contains("^^"))
						labl = labl.substring(0, labl.length()-2);
					//	System.out.println(labl+" "+thisClass);
					ontPropertiess.put(labl, thisClass);
				}
			}
		}
	}
	private void loadClasses(){
		ExtendedIterator<OntClass> iter = model.listClasses();
		while (iter.hasNext()) {
			OntClass thisClass = (OntClass) iter.next();
			ExtendedIterator<?> label = thisClass.listLabels(null);
			while (label.hasNext()) {
				RDFNode thisLabel = (RDFNode) label.next();
				if(thisLabel.isLiteral()){
					if(!thisLabel.toString().contains("geosparql")){
						String labl = thisLabel.toString().split("http")[0].replaceAll("@en", "").replaceAll("@pt", "").toLowerCase();
						labl = Normalizer.normalize(labl, Normalizer.Form.NFD);  
						labl = labl.replaceAll("[^\\p{ASCII}]", "");
						labl = labl.replaceAll("^^", "");
						if(labl.contains("^^"))
							labl = labl.substring(0, labl.length()-2);
						ontClasses.put(labl, thisClass);
					}
				}
			}
		}
	}


	private OntClass useMoreGeneric(List<OntClass> cl) {
		return cl.get(0);
	}
	private OntClass getMoreSpecifc(List<OntClass> cl) {
		OntClass first = cl.get(0);
		boolean find = false;
		for(int i=1;i<cl.size();i++){
			//Answer true if the given class is a sub-class of this class.
			if(cl.get(i).hasSubClass(first)){
				first = cl.get(i);
				find = true;
				//Answer true if the given class is a sub-class of this class.
			}else if(first.hasSubClass(cl.get(i))){
				first = cl.get(i);
				find = true;
			}
		}
		if(find)
			return first;		
		if(!find && cl.size()>=1)
			return cl.get(cl.size()-1);
		return null;
	}


	@Override
	public HashMap<String,Locality> searchLocalities(String search, OntModel model,double similarity,String endpoint) throws Exception {
		
		HashMap<String,Locality> result = new HashMap<String,Locality>();
		System.out.println(search);

		HashMap<OntClass,String> identified = new HashMap<OntClass,String>();

		if(model==null){
			String ontologyIRI = "files"+File.separator+"Gazetteer_v_1_1.owl";
			File f = new File(ontologyIRI);
			System.out.println(f.getAbsolutePath());
			OntModel m = ModelFactory.createOntologyModel();
			InputStream in = FileManager.get().open(ontologyIRI);

			GazetteerServiceImpl.model = (OntModel) m.read(in,"");
			//model = loadOntology();
		
		}else{
			GazetteerServiceImpl.model=model;
		}
		loadClasses();
		loadProperties();
	
		if(similarity>0){
			this.treshold=similarity;
		}
		if(!endpoint.equals("")){
			this.URL_endpoint=endpoint;
		}
		List<OntClass> cl = new ArrayList<OntClass>();
		List<OntProperty> properties = new ArrayList<OntProperty>();
		cl.addAll(findClasses(search.toLowerCase()));		
		properties.addAll(findProperties(search));
		System.out.println("Classes: "+cl.size()+" Propriedades: "+properties.size());
		if(cl.size()>1){
			identified = linkClassePlaceNames(search,properties,cl);
			OntClass moreGeneric = useMoreGeneric(cl);
			OntClass moreSpecific = getMoreSpecifc(cl);
			System.out.println("MoreGeneric:  "+moreGeneric.toString());
			System.out.println("MoreSpecific: "+moreSpecific.toString());
			result.putAll(findIntersection(moreGeneric,moreSpecific,properties,identified));
			result.putAll(findProximity(moreGeneric,moreSpecific,properties,identified,search));
			cl.remove(moreSpecific);

		}else{
			result.putAll(findSinglePlace(cl,search.toLowerCase()));
		}

		return result;
	}

	private Collection<? extends OntClass> findClasses(String search) {
		List<OntClass> cl = new ArrayList<OntClass>();
		String temp [] =search.toLowerCase().split(" "); 
		for(int n=0;n<temp.length-1;n++){
			String tipo = temp[n].toLowerCase().trim()+" "+temp[n+1].toLowerCase().trim();
			if(ontClasses.containsKey(tipo)){
				cl.add(ontClasses.get(tipo));
			}else if(ontClasses.containsKey(temp[n])){
				cl.add(ontClasses.get(temp[n]));
			}
		}
		return cl;
	}

	private Collection<? extends OntProperty> findProperties(String search) {
		List<OntProperty> properties = new ArrayList<OntProperty>();
		String temp [] =search.toLowerCase().split(" "); 
		for(int n=0;n<temp.length-1;n++){
			String tipo = temp[n].toLowerCase().trim()+" "+temp[n+1].toLowerCase().trim();
			if(ontPropertiess.containsKey(tipo)){
				properties.add(ontPropertiess.get(tipo));
			}else if(ontPropertiess.containsKey(temp[n])){
				properties.add(ontPropertiess.get(temp[n]));
			}
		}

		return properties;
	}

	private boolean notProperty(List<OntProperty> pro,String word){
		for(OntProperty p:pro){
			ExtendedIterator<?> label = p.listLabels(null);
			while (label.hasNext()) {
				RDFNode thisLabel = (RDFNode) label.next();
				if(thisLabel.isLiteral()){
					String labl = thisLabel.toString().split("http")[0].replaceAll("@en", "").replaceAll("@pt", "").toLowerCase();
					labl = Normalizer.normalize(labl, Normalizer.Form.NFD);  
					labl = labl.replaceAll("[^\\p{ASCII}]", "");
					labl = labl.replaceAll("^^", "");
					if(labl.contains("^^"))
						labl = labl.substring(0, labl.length()-2);
					//	System.out.println(labl);
					if(word.equalsIgnoreCase(labl))
						return false;
				}
			}
		}
		return true;
	}

	private boolean notClasse(String word, List<OntClass> classes){
		for(OntClass p:classes){
			ExtendedIterator<?> label = p.listLabels(null);
			while (label.hasNext()) {
				RDFNode thisLabel = (RDFNode) label.next();
				if(thisLabel.isLiteral()){
					if(!thisLabel.toString().contains("geosparql")){
						String labl = thisLabel.toString().split("http")[0].replaceAll("@en", "").replaceAll("@pt", "").toLowerCase();
						labl = Normalizer.normalize(labl, Normalizer.Form.NFD);  
						labl = labl.replaceAll("[^\\p{ASCII}]", "");
						labl = labl.replaceAll("^^", "");
						if(labl.contains("^^"))
							labl = labl.substring(0, labl.length()-2);
						if(word.equalsIgnoreCase(labl)){
							return false;
						}
					}
				}
			}
		}
		return true;
	}

	private String removeStopWord(String name){
		String temp[] = name.split(" ");
		String place ="";
		for(int i=0;i<temp.length;i++){		
			if(not_stop_word(temp[i],stop_words))
				place+=temp[i]+" ";
		}
		if(place.length()>1)
			return place.substring(0, place.length()-1);
		else
			return "";
	}

	private HashMap<OntClass,String> linkClassePlaceNames(String search,List<OntProperty> property,List<OntClass>classes) throws FileNotFoundException, IOException {
		HashMap<OntClass,String> identified = new HashMap<OntClass,String>();
		String temp[] = search.split(" ");
		stop_words = read_stop_words("files"+File.separator+"stop_words.txt");
		String place ="";
		OntClass fist=null,atual= classes.get(0);
		for(int i=0;i<temp.length;i++){				
			if(not_stop_word(temp[i],stop_words) && notClasse(temp[i],classes) && notProperty(property,temp[i]) && notNumber(temp[i])){
				place += temp[i] +" ";
			}else if (!notClasse(temp[i],classes) && !place.trim().equalsIgnoreCase("")){
				identified.put(fist, place);
				place = "";
			} else if (!notClasse(temp[i],classes)){
				fist = atual;
				int index = classes.indexOf(fist)+1;
				if(index<classes.size()){
					atual = classes.get(index);				
				}

			}	
		}		
		identified.put(atual,place);

		return identified;
	}

	private boolean notNumber(String string) {
		try{
			Pattern p = Pattern.compile("-?\\d+");
			Matcher m = p.matcher(string);
			while (m.find()) {
				Integer.parseInt(m.group());
				return false;
			}
		}catch(Exception e){

		}
		return true;
	}

	private ArrayList<String> read_stop_words(String file_path) throws FileNotFoundException, IOException {
		String word ="";
		ArrayList<String> uselles = new ArrayList<String>();
		BufferedReader lines =  new BufferedReader(new InputStreamReader(new FileInputStream(file_path), "UTF-8"));
		while ((word = lines.readLine()) != null) {
			uselles.add(word);
		}
		lines.close();
		return uselles;
	}

	private boolean  not_stop_word(String word,ArrayList<String> stop_words){
	
		String localtemp = Normalizer.normalize(word, Normalizer.Form.NFD);  
		localtemp = localtemp.replaceAll("(?!\")\\p{Punct}", "");
		for (String s : stop_words) {
			if(localtemp.equals(s))
				return false;
		}

		return true;     
	}
	private HashMap<String,Locality> findSinglePlace(List<OntClass> cl,String search) throws Exception {
		//try find objects from the first type found
		System.out.println("SINGLE search begin...");


		HashMap<String,Locality> list = new HashMap<String,Locality>();
		String queryString="PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>"
				+ " PREFIX geo: <http://www.opengis.net/ont/geosparql#> "
				+ " PREFIX geof: <http://www.opengis.net/def/function/geosparql/>"
				+ " PREFIX swi: <http://www.semanticweb.org/ontologies/Gazetter#> "
				+ " PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> ";
		if(!cl.isEmpty()){
			queryString+= " SELECT (COUNT(?s) AS ?NumOfTriples)  WHERE{ ?s a <"+cl.get(0).toString()+">. ";
		}else{
			queryString+= " SELECT (COUNT(?s) AS ?NumOfTriples)  WHERE{ ?s ?p ?o .";
		}
		queryString+= " ?s swi:locality ?local1 . "
				+ " ?s geo:hasGeometry ?geo1 . "
				+ " ?geo1 geo:asWKT ?wkt1 . }";
		System.out.println(queryString);
		com.hp.hpl.jena.query.Query query = QueryFactory.create(queryString) ;
		QueryEngineHTTP queryExecution=new QueryEngineHTTP(URL_endpoint,query);

		ResultSet results= queryExecution.execSelect();

		int limit=0;
		while(results.hasNext()) {
			QuerySolution soln = results.nextSolution() ;
			limit=soln.get("NumOfTriples").asLiteral().getInt();
		}
		int offset=0;
		int count=0;
		while(count<limit){
			count +=100;
			queryString="PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>"
					+ " PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>"
					+ " PREFIX geo: <http://www.opengis.net/ont/geosparql#> "
					+ " PREFIX geof: <http://www.opengis.net/def/function/geosparql/>"
					+ " PREFIX swi: <http://www.semanticweb.org/ontologies/Gazetter#> "
					+ " PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> ";
			if(!cl.isEmpty()){
				queryString+= " SELECT * WHERE{ ?s a <"+cl.get(0).toString()+">. ";
			}else{
				queryString+= " SELECT *  WHERE{ ?s ?p ?o .";
			}
			queryString+= " ?s swi:locality ?local1 . "
					+ " ?s geo:hasGeometry ?geo1 . "
					+ " ?s swi:contributors ?cont1 . "
					+ " ?s swi:agreement ?agree1 . "
					+ " ?s rdf:type ?type . "
					+ " ?geo1 geo:asWKT ?wkt1 . } limit "+limit+" offset "+offset;
		//	System.out.println(queryString);
			query = QueryFactory.create(queryString) ;
			queryExecution=new QueryEngineHTTP(URL_endpoint,query);

			results= queryExecution.execSelect();
		
			try {

				while(results.hasNext()) {
					QuerySolution soln = results.nextSolution() ;
					String place = soln.get("local1").asLiteral().getString();
					place = removeStopWord(place);
					if(similarityBettewenplaces(search,place)){
						//	System.out.println(soln);
							Locality l = new Locality();						
								l.setLocality(soln.get("local1").asLiteral().getString());
								l.setGeometry(soln.getLiteral("wkt1").asLiteral().getString());
								l.setIdGeo(soln.getResource("geo1").toString());
								l.setIdTriple(soln.getResource("s").toString());
								l.setDate(soln.get("cont1").asLiteral().getString());
								l.setDate(soln.get("agree1").asLiteral().getString());
								l.setType(soln.get("type").toString());
								Bigram bigram = new Bigram();
								l.setScore(bigram.stringSimilarityScore(bigram.bigram(search), bigram.bigram(place)));
								if(!list.containsKey(l.getIdTriple()))
									list.put(l.getIdTriple(),l);
					
					}
				}
				
			}catch(Exception ex){}

		}
		System.out.println("SINGLE search Finish!");
		return list;
	}

	private HashMap<String,Locality> findProximity(OntClass moreGeneric, OntClass moreSpecific,List<OntProperty> property,HashMap<OntClass, String> identified,String search) throws Exception{
		String queryString="";
		HashMap<String,Locality> result = new HashMap<String,Locality>();
		System.out.println("Begin proximity....");
		//String diretional [] ={"southeast","southwest","northwest","northeast","west","south","north","east"};
		
		for(OntProperty ot:property){
			//System.out.println(ot.toString().split("#")[1]);
			if(ot.toString().split("#")[1].equals("near")){
				int distance = findistance(search,property);
				if(distance==0)
					distance =300;
				String wkt = pointMoreSpecific(identified,moreSpecific);
				//System.out.println(wkt);
				if(wkt!=null){
					//calcule distance
					queryString="PREFIX geof:<http://www.opengis.net/def/function/geosparql/>"
							+ "PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#>"
							+ "PREFIX geo:<http://www.opengis.net/ont/geosparql#>"
							+ "PREFIX opengis: <http://www.opengis.net/def/uom/OGC/1.0/>"
							+ "PREFIX swi: <http://www.semanticweb.org/ontologies/Gazetter#>"
							+ "SELECT *";
					if(!moreGeneric.equals(ontClasses.get("locais"))){
						queryString += " WHERE { ?s a <"+moreGeneric.toString()+"> ."
								+ "        ?s geo:hasGeometry ?s1 ."
								+ "	?s1 geo:asWKT ?o1 ."
								+ "        ?s swi:locality ?locality ."
								+ " ?instance swi:contributors ?cont1 . "
								+ " ?instance rdf:type ?type . "
								+ " ?instance swi:agreement ?agree1 . ";

						if(identified.containsKey(moreGeneric)){
							queryString+= " FILTER regex(str(?locality), \"locality\"). "
									+ "	 FILTER(geof:sfWithin(?o1, geof:buffer("+wkt+", "+distance+", opengis:metre))).}";
						}else{
							queryString += "	 FILTER(geof:sfWithin(?o1, geof:buffer("+wkt+", "+distance+", opengis:metre))).}";
						}
					}else{
						queryString += " WHERE { ?s geo:hasGeometry ?s1 ."
								+ "	?s1 geo:asWKT ?o1 ."
								+ "        ?s swi:locality ?locality ."
								+ "	 FILTER(geof:sfWithin(?o1, geof:buffer("+wkt+", "+distance+", opengis:metre))).}";
					}


					System.out.println(queryString);
					com.hp.hpl.jena.query.Query query = QueryFactory.create(queryString) ;
					QueryEngineHTTP queryExecution=new QueryEngineHTTP(URL_endpoint,query);

					ResultSet results= queryExecution.execSelect();
					try {
					

						while(results.hasNext()) {
							QuerySolution soln = results.nextSolution() ;
							Locality l = new Locality();
								l.setLocality(soln.get("local1").asLiteral().getString());
								l.setGeometry(soln.getLiteral("wkt1").asLiteral().getString());
								l.setIdGeo(soln.getResource("geo1").toString());
								l.setIdTriple(soln.getResource("instance").toString());
								l.setDate(soln.get("cont1").asLiteral().getString());
								l.setDate(soln.get("agree1").asLiteral().getString());
								l.setType(soln.get("type").toString());
							
								if(!result.containsKey(l.getIdTriple()))
									result.put(l.getIdTriple(),l);

						}
					
					}catch(Exception ex){}
				}
			}
		}
		System.out.println("Finish proximity!");
		return result;
	}
	
	private String pointMoreSpecific(HashMap<OntClass, String> identified, OntClass morespecifc) throws Exception {

	
		String join="";
		Set<OntClass>  key = identified.keySet();
		Iterator<OntClass> it = key.iterator();
		String	queryString ="";
		while(it.hasNext()){
			OntClass temp = it.next();
			if(morespecifc.equals(temp)){
				join = identified.get(temp);
				queryString="PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>"
						+ " PREFIX geo: <http://www.opengis.net/ont/geosparql#> "
						+ " PREFIX geof: <http://www.opengis.net/def/function/geosparql/>"
						+ " PREFIX swi: <http://www.semanticweb.org/ontologies/Gazetter#> "
						+ " PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> "
						+ " SELECT * WHERE{ ?s a <"+temp.toString()+">. "
						+ "  ?s swi:county ?c."
						+ " ?s swi:hasGeometry ?geo ."
						+ " ?geo geo:asWKT ?wkt. " ;
			}
		}
		if(identified.containsKey(morespecifc)){
			String temp = identified.get(morespecifc);
			temp = temp.substring(0, temp.length()-1);
			queryString+=" FILTER(STRSTARTS(lcase(STR(?c)), lcase(\""+temp+"\"))). }";
		}else{
			queryString+="}";
		}

		if(!queryString.equalsIgnoreCase("")){
			com.hp.hpl.jena.query.Query query = QueryFactory.create(queryString) ;
			QueryEngineHTTP queryExecution=new QueryEngineHTTP(URL_endpoint,query);

			ResultSet results= queryExecution.execSelect();

			while(results.hasNext()) {

				QuerySolution soln = results.nextSolution() ;
				//	System.out.println(soln);
				Literal local1 = soln.getLiteral("?c");

				String place = local1.getString().replaceAll("<http://www.w3.org/2001/XMLSchema#string>", "");
				place = removeStopWord(place);
				HashMap<String,String> triplestore=new HashMap<String,String>();
				triplestore.put("locality","?c");
				triplestore.put("geo", "?wkt");
				if(similarityBettewenplaces(join,place)){
					Locality l = new Locality();
					l.setGeometry(soln.get("wkt").asLiteral().getString());
					if(l.getGeometry().contains("POLYGON"))
						l.setGeometry(changeCoords(l.getGeometry()));
//					System.out.println(l.getGeometry());
					return "\""+l.getGeometry()+";http://www.opengis.net/def/crs/EPSG/0/4326\"^^<http://strdf.di.uoa.gr/ontology#WKT>";
				}

			}
		}
		return null;
	}

	private String changeCoords(String geometry) {
		Out_Polygon out = new Out_Polygon();		 
		return out.buildPolygon(geometry,true);
	}

	private int findistance(String search,List<OntProperty> property) {
		List<Integer> values = new ArrayList<Integer>();
		try{
			String [] temp = search.split(" ");
			for(int i=0;i<temp.length;i++){
				Pattern p = Pattern.compile("-?\\d+");
				Matcher m = p.matcher(temp[i]);
				while (m.find()) {
					values.add(Integer.parseInt(m.group()));
				}
			}
		}catch(Exception e){

		}
		int distance =0;
		for(int temp:values){
			distance+=temp;
		}
		String temp[] = search.split(" ");
		for(int i=0;i<temp.length;i++){
			if(temp[i].equals("km")){
				distance=distance*1000;
				break;
			}
		}
		return distance;
	}


	private Locality searchSingle( HashMap<OntClass, String> identified,OntClass moreGeneric) throws Exception{
		String queryString="";
		Locality l = new Locality();
		if (moreGeneric.toString().equals("http://schema.org/City")){
			String wkt = pointMoreSpecific(identified,moreGeneric);
			l.setGeometry(wkt);
			return l; 
		}else if(identified.containsKey(moreGeneric)){


			queryString=" PREFIX geo: <http://www.opengis.net/ont/geosparql#> "
					+ " PREFIX geof: <http://www.opengis.net/def/function/geosparql/> "
					+ " PREFIX swi: <http://www.semanticweb.org/ontologies/Gazetter#> "
					+ " PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> "
					+ " SELECT (COUNT(?instance2) AS ?NumOfTriples) WHERE{  ?instance a <"+moreGeneric.toString()+"> . "
					+ " ?instance2 geo:hasGeometry ?geo1 . }";
			com.hp.hpl.jena.query.Query query = QueryFactory.create(queryString) ;
			QueryEngineHTTP queryExecution=new QueryEngineHTTP(URL_endpoint,query);
			ResultSet results= queryExecution.execSelect();
			int limit=0;
			while(results.hasNext()) {
				QuerySolution soln = results.nextSolution() ;
				limit=soln.get("NumOfTriples").asLiteral().getInt();
			}
			int offset=0;
			int count=0;
			while(count<limit){
				count +=100;
				queryString =" PREFIX geo: <http://www.opengis.net/ont/geosparql#>"
						+ " PREFIX geof: <http://www.opengis.net/def/function/geosparql/>"
						+ " PREFIX swi: <http://www.semanticweb.org/ontologies/Gazetter#>"
						+ " PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>"
						+ " SELECT * WHERE{"
						+ " ?instance2 a <"+moreGeneric.toString()+"> ."
						+ " ?instance2 geo:hasGeometry ?geo2 ."
						+ " ?instance2 swi:locality ?local2 . "
						+ " ?instance2 geo:hasGeometry ?geo2 . "
						+ " ?geo2 geo:asWKT ?wkt2 .} limit"+count+" offset "+offset;
				query = QueryFactory.create(queryString) ;
				queryExecution=new QueryEngineHTTP(URL_endpoint,query);
				results= queryExecution.execSelect();
				while(results.hasNext()) {
					QuerySolution soln = results.nextSolution() ;
					String name=soln.get("local2").asLiteral().getString();
					String geo = soln.get("wkt2").asLiteral().getString();
					name = removeStopWord(name);
					if(similarityBettewenplaces(name,identified.get(moreGeneric))){
						if(geo.contains("POLYGON")){
							l.setIdTriple(soln.get("instance2").asResource().getURI());
							l.setIdGeo(soln.get("geo2").asResource().getURI());
							l.setLocality(name);
							l.setGeometry(geo);
							if(!moreGeneric.toString().equals("http://schema.org/State")){
								l.setGeometry(changeCoords(l.getGeometry()));							 	
							}
							l.setGeometry("\""+l.getGeometry()+";http://www.opengis.net/def/crs/EPSG/0/4326\"^^<http://strdf.di.uoa.gr/ontology#WKT>");
							count = limit+1;
							break;
						}
					}
				}
				offset+=count;
			}
			return l;
		}
		return null;
	}

	private HashMap<String,Locality> findIntersection(OntClass moreGeneric,OntClass moreSpecific,List<OntProperty> properties, HashMap<OntClass, String> identified) throws Exception{
		HashMap<String,Locality> result = new HashMap<String,Locality>();
		boolean execute = false;
		
		for(OntProperty ot:properties){
			if(ot.toString().split("#")[1].equals("sfWithin") || ot.toString().split("#")[1].equals("sfIntersects")){
				execute = true;
			}
		}
		if(properties.isEmpty())
			execute = true;
		
		
		if(execute){
			System.out.println("Begin Intersection....");
			Locality local1 = searchSingle(identified,moreGeneric);
			Locality local2 = searchSingle(identified,moreSpecific);

			String queryString="";
			queryString=" PREFIX geo: <http://www.opengis.net/ont/geosparql#> "
					+ " PREFIX geof: <http://www.opengis.net/def/function/geosparql/> "
					+ " PREFIX swi: <http://www.semanticweb.org/ontologies/Gazetter#> "
					+ " PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> ";
			if(!moreGeneric.equals(ontClasses.get("locais")) && local1==null){
				queryString += " SELECT * WHERE{  ?instance a <"+moreGeneric.toString()+"> . "
						+ " ?instance geo:hasGeometry ?geo1 . "
						+ " ?instance swi:locality ?local1 . "
						+ " ?instance geo:hasGeometry ?geo1 . "
						+ " ?geo1 geo:asWKT ?wkt1 . "
						+ " ?instance swi:contributors ?cont1 . "
						+ " ?instance rdf:type ?type . "
						+ " ?instance swi:agreement ?agree1 . ";
			}else if(local1==null){
				queryString	+= " SELECT * WHERE{  "
						+ " ?instance geo:hasGeometry ?geo1 . "
						+ " ?instance swi:locality ?local1 . "
						+ " ?instance geo:hasGeometry ?geo1 . "
						+ " ?geo1 geo:asWKT ?wkt1 . "
						+ " ?instance swi:contributors ?cont1 . "
						+ " ?instance rdf:type ?type . "
						+ " ?instance swi:agreement ?agree1 . ";
			}

			if(!moreSpecific.equals(ontClasses.get("locais")) && local2==null){
				queryString+=" ?instance2 a <"+moreSpecific.toString()+"> . "
						+ " ?instance2 geo:hasGeometry ?geo2 . "
						+ " ?instance2 swi:locality ?local2 . "
						+ " ?instance2 geo:hasGeometry ?geo2 . "
						+ " ?geo2 geo:asWKT ?wkt2 . "
						+ " ?instance2 swi:contributors ?cont2 . "
						+ " ?instance2 rdf:type ?type . "
						+ " ?instance2 swi:agreement ?agree2 . ";
			}else if(local2==null){
				queryString+=" ?instance2 geo:hasGeometry ?geo2 . "
						+ " ?instance2 swi:locality ?local2 . "
						+ " ?instance2 geo:hasGeometry ?geo2 . "
						+ " ?geo2 geo:asWKT ?wkt2 . "
						+ " ?instance2 swi:contributors ?cont2 . "
						+ " ?instance2 rdf:type ?type . "
						+ " ?instance2 swi:agreement ?agree2 . ";
			}
			if(local1==null)
				queryString+= " FILTER(geof:sfIntersects(?wkt1,"+local2.getGeometry()+" )). }";
			else if(local2==null)
				queryString+= " FILTER(geof:sfIntersects(?wkt2,"+local1.getGeometry()+" )). }";
			else
				queryString+= " FILTER(geof:intersects(?wkt1,?wkt2)). }";
		
			System.out.println(queryString);
			
			com.hp.hpl.jena.query.Query query = QueryFactory.create(queryString) ;
			QueryEngineHTTP queryExecution=new QueryEngineHTTP(URL_endpoint,query);

			ResultSet results= queryExecution.execSelect();
		
			try {
				while(results.hasNext()) {
					QuerySolution soln = results.nextSolution() ;
					//System.out.println(soln);
					Locality l = new Locality();
					if(soln.get("local1").asLiteral()!=null){
						l.setLocality(soln.get("local1").asLiteral().getString());
						l.setGeometry(soln.getLiteral("wkt1").asLiteral().getString());
						l.setIdGeo(soln.getResource("geo1").toString());
						l.setIdTriple(soln.getResource("instance").toString());
						l.setType(soln.getLiteral("type").toString());
						l.setDate(soln.get("cont1").asLiteral().getString());
						l.setDate(soln.get("agree1").asLiteral().getString());
					}else{
						l.setLocality(soln.get("local2").asLiteral().getString());
						l.setGeometry(soln.getLiteral("wkt2").asLiteral().getString());
						l.setIdGeo(soln.getResource("geo2").toString());
						l.setIdTriple(soln.getResource("instance2").toString());
						l.setType(soln.getLiteral("type").toString());
						l.setDate(soln.get("cont2").asLiteral().getString());
						l.setDate(soln.get("agree2").asLiteral().getString());
					}
					if(!result.containsKey(l.getIdTriple()))
						result.put(l.getIdTriple(), l);
				}
			
			}catch(Exception ex){}
		}
		System.out.println("Finish Intersection");
		return result;
	}


	private boolean similarityBettewenplaces(String search, String place) throws Exception {

		Bigram bigram = new Bigram();
		if(bigram.stringSimilarityScore(bigram.bigram(search), bigram.bigram(place))>=treshold){
			return true;
		}
		return false;
	}


	@Override
	public Integer insertLocality(Locality locality,String user,String password,String endpoint) {
		SPRQLQuery spql = new SPRQLQuery();
		if(locality==null)
			return 0;
		
		if(locality.getGeometry().contains("POINT")){
			String value = locality.getGeometry().replaceAll(";http://www.opengis.net/def/crs/EPSG/0/4326", "");			
			value = value.substring(6, value.length()-2);
		
		}

		if(!spql.askService())
			return 0;
		if(!locality.isUpdateGeo()){
			int individual = spql.getIndex();
			String query = "DELETE WHERE{ <"+locality.getIdTriple()+"> ?p ?o .} ; ";
			query += " INSERT DATA { <"+locality.getIdTriple()+"> <http://www.semanticweb.org/ontologies/Gazetter#contributors> \""+locality.getContributors()+"\"^^<http://www.w3.org/2001/XMLSchema#long> . ";
			query += "<"+locality.getIdTriple()+"> <http://www.semanticweb.org/ontologies/Gazetter#agreement> \""+locality.getAgreeCoordinate()+"\"^^<http://www.w3.org/2001/XMLSchema#long> . ";
			query +="<"+locality.getIdTriple()+"> <http://www.semanticweb.org/ontologies/Gazetter#infotype> \"user\"^^<http://www.w3.org/2001/XMLSchema#string> . ";
			query += "<"+locality.getIdTriple()+"> <http://www.semanticweb.org/ontologies/Gazetter#locality> \""+locality.getLocality()+"\"^^<http://www.w3.org/2001/XMLSchema#string> . ";
			query += "<"+locality.getIdTriple()+"> <http://www.semanticweb.org/ontologies/Gazetter#county> \""+locality.getCounty()+"\"^^<http://www.w3.org/2001/XMLSchema#string> . ";
			query += "<"+locality.getIdTriple()+"> <http://www.semanticweb.org/ontologies/Gazetter#ntriples> \""+locality.getNtriplas()+"\"^^<http://www.w3.org/2001/XMLSchema#long> . ";
			query += "<"+locality.getIdTriple()+"> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <"+locality.getType()+"> . ";
			query+="<"+locality.getIdTriple()+"> <http://www.opengis.net/ont/geosparql#hasGeometry> <http://www.semanticweb.org/ontologies/Gazetter#"+(individual)+"> . ";
			query += " <http://www.semanticweb.org/ontologies/Gazetter#"+(individual)+"> <http://www.opengis.net/ont/geosparql#asWKT> \""+locality.getGeometry()+";http://www.opengis.net/def/crs/EPSG/0/4326\"^^<http://strdf.di.uoa.gr/ontology#WKT> . ";
			query += "<http://www.semanticweb.org/ontologies/Gazetter#"+(individual)+"> <http://www.semanticweb.org/ontologies/Gazetter#date> \""+locality.getDate()+"\"^^<http://www.w3.org/2001/XMLSchema#long> . ";
			query +="<http://www.semanticweb.org/ontologies/Gazetter#"+(individual)+">  <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://www.opengis.net/ont/sf#Geometry>  . }";
			query+=" }";
			System.out.println(query);
			spql.insertDataEndpoint(query,user,password,endpoint);
		}else{
			int individual = spql.getTempIndex();
			individual++;
			String val [] = locality.getIdTriple().split("#");
			String tripleID = val[0]+"/temp/"+val[1];
			String query = "INSERT DATA {"
					+ " GRAPH <http://swigazetteer/temp> { <"+tripleID+"> <http://swigazetteer/temp/ontology/hasGeometry> <http://swigazetteer/temp/id/"+individual+"> ."
					+ "	<http://swigazetteer/temp/id/"+individual+">  <http://www.opengis.net/ont/geosparql#asWKT> \""+locality.getGeometry()+";http://www.opengis.net/def/crs/EPSG/0/4326\"^^<http://strdf.di.uoa.gr/ontology#WKT> ."
					+ " <http://swigazetteer/temp/id/"+individual+">  <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> \"<http://www.opengis.net/ont/sf#Point>\" ."
					+ " <http://swigazetteer/temp/id/"+individual+">  <http://swigazetteer/temp#date> \""+locality.getDate()+"\"^^<http://www.w3.org/2001/XMLSchema#long> .	} }";

			System.out.println(query);
			spql.insertDataEndpoint(query,user,password,endpoint);
			Geo center = spql.getCentertemp(tripleID);
			if(center!=null){
				String geo = "POINT("+center.getLatitude()+" "+center.getLongitude()+")";
				query ="DELETE WHERE{ <"+locality.getIdGeo()+">  <http://www.opengis.net/ont/geosparql#asWKT> ?a . ";
				query+="<"+locality.getIdTriple()+">  <http://www.semanticweb.org/ontologies/Gazetter#agreement> ?b . ";
				query+="<"+locality.getIdTriple()+">  <http://www.semanticweb.org/ontologies/Gazetter#infotype> ?c . ";
				query+="<"+locality.getIdTriple()+">  <http://www.semanticweb.org/ontologies/Gazetter#locality> ?d . ";
				query+="<"+locality.getIdTriple()+">  <http://www.semanticweb.org/ontologies/Gazetter#county> ?e . }; ";
				query += "INSERT DATA { <"+locality.getIdGeo()+"> <http://www.opengis.net/ont/geosparql#asWKT> \""+geo+";http://www.opengis.net/def/crs/EPSG/0/4326\"^^<http://strdf.di.uoa.gr/ontology#WKT> . ";
				query += "<"+locality.getIdTriple()+"> <http://www.semanticweb.org/ontologies/Gazetter#agreement> \""+locality.getAgreeCoordinate()+"\"^^<http://www.w3.org/2001/XMLSchema#long> . ";
				query +="<"+locality.getIdTriple()+"> <http://www.semanticweb.org/ontologies/Gazetter#infotype> \"user\"^^<http://www.w3.org/2001/XMLSchema#string> . ";
				query += "<"+locality.getIdTriple()+"> <http://www.semanticweb.org/ontologies/Gazetter#locality> \""+locality.getLocality()+"\"^^<http://www.w3.org/2001/XMLSchema#string> . ";
				query += "<"+locality.getIdTriple()+"> <http://www.semanticweb.org/ontologies/Gazetter#county> \""+locality.getCounty()+"\"^^<http://www.w3.org/2001/XMLSchema#string> . ";
				query += "<"+locality.getIdTriple()+"> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <"+locality.getType()+"> . }";

				System.out.println(query);
				spql.insertDataEndpoint(query,user,password,endpoint);
			}
		}


		return 1;
	}

	@Override
	public void agreeLinkedData(Locality locality,String user,String password,String endpoint) {
		SPRQLQuery spql = new SPRQLQuery();
		String query = "DELETE where{ <"+locality.getIdTriple()+"> <http://www.semanticweb.org/ontologies/Gazetter#agreement> ?o .}; "
				+ "INSERT DATA { <"+locality.getIdTriple()+"> <http://www.semanticweb.org/ontologies/Gazetter#agreement> \""+locality.getAgreeCoordinate()+"\"^^<http://www.w3.org/2001/XMLSchema#long> . }";
		System.out.println(query);
		spql.insertDataEndpoint(query,user,password,endpoint);
		query = "DELETE where{ <"+locality.getIdTriple()+"> <http://www.semanticweb.org/ontologies/Gazetter#contributors> ?o .}; "
				+ "INSERT DATA { <"+locality.getIdTriple()+"> <http://www.semanticweb.org/ontologies/Gazetter#contributors> \""+locality.getContributors()+"\"^^<http://www.w3.org/2001/XMLSchema#long> . }";	
		System.out.println(query);
		spql.insertDataEndpoint(query,user,password,endpoint);
	}


	@Override
	public List<Float[]> getPolygons(String endpoint){
		final List<Float[]> points = new ArrayList<Float[]>();
		Out_Polygon out = new Out_Polygon();
		SPRQLQuery spql = new SPRQLQuery();

		if(!spql.askService())
			return points;

		if(resultGeo == null){

			resultGeo=spql.getGeometriesInsideTripleStore(endpoint);
		}

		System.out.println("Vai construir os pontos");
		//create a semi-random grid of features to be clustered
		Iterator<Locality> iterator = resultGeo.iterator();
		while(iterator.hasNext()){
			Locality loc =  iterator.next();
			if(loc.getGeometry().contains("POINT")){
				String value = loc.getGeometry().replaceAll(";http://www.opengis.net/def/crs/EPSG/0/4326", "");
				//     	fw.write(value+"\n");
				value = value.substring(7, value.length()-2);
				float x = out.transformFloat(value.split(" ")[0]);
				float y = out.transformFloat(value.split(" ")[1]);
				points.add(new Float []{ y,x});
			}
		}
		return points;
	}

	@Override
	public List<String> getOntTypes(String endpoint) {
		SPRQLQuery spql = new SPRQLQuery();
		if(classes==null){
			classes = spql.getTypes(endpoint);
		}
		return classes;
	}

	@Override
	public String[] infoServer(String endpoint) {
		if(!isRead){
			SPRQLQuery spql = new SPRQLQuery();
			if(spql.askService() && valueCoord==null){
				String [] valueCoord = spql.getInfo(endpoint);

				return valueCoord;
			}
		}
		return valueCoord;
	}

}
