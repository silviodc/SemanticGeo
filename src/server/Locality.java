package server;


import java.io.Serializable;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
 
@XmlRootElement
public class Locality implements Serializable  {

	private static final long serialVersionUID = 1L;

	/*
	 * [0] => placeSlected (String)
	 * [1] => agree_text (String)  ok
	 * [2] => place_text (String) ok
	 * [3] => county_text (String) ok
	 * [4] => species_text (String) ok
	 * [5] => communCoordinate (String) ok
	 * [6] => placeLink (String) ok
	 * 
	 */
	private String locality;
	private String county="";
	private String geometry="";
	private int agreeCoordinate;
	private int contributors;
	private String idTriple;
	private String date="";
	private String type;
	private boolean updateGeo=false;
	private String idGeo="";
	private String ntriplas;
	private double score;
	
	


	public Locality(){}
	
	public Locality(String locality, String geo, String county, int agree,String date, int contributors, String link,String type) {
		this.locality = locality;
		this.geometry = geo;
		this.agreeCoordinate = agree;
		this.contributors = contributors;
		this.idTriple = link;
		this.county = county;
		this.date = date;
		this.type = type;
	}
	@XmlElement
	public String getLocality() {
		return locality;
	}
	@XmlElement
	public double getScore() {
		return score;
	}
	@XmlElement
	public void setScore(double score) {
		this.score = score;
	}
	@XmlElement
	public String getNtriplas() {
		return ntriplas;
	}
	@XmlElement
	public void setNtriplas(String ntriplas) {
		this.ntriplas = ntriplas;
	}
	@XmlElement
	public void setLocality(String locality) {
		this.locality = locality;
	}
	@XmlElement
	public String getCounty() {
		return county;
	}
	@XmlElement
	public void setCounty(String county) {
		this.county = county;
	}
	@XmlElement
	public String getType() {
		return type;
	}
	@XmlElement
	public void setType(String type) {
		this.type = type;
	}
	@XmlElement
	public String getGeometry() {
		return geometry;
	}
	@XmlElement
	public void setGeometry(String geometry) {
		this.geometry = geometry;
	}
	@XmlElement
	public int getContributors() {
		return contributors;
	}
	@XmlElement
	public void setContributors(int contributors) {
		this.contributors = contributors;
	}
	@XmlElement
	public String getDate() {
		return date;
	}
	@XmlElement
	public void setDate(String date) {
		this.date = date;
	}
	@XmlElement
	public int getAgreeCoordinate() {
		return agreeCoordinate;
	}
	@XmlElement
	public void setAgreeCoordinate(int agreeCoordinate) {
		this.agreeCoordinate = agreeCoordinate;
	}
	@XmlElement
	public String getPlaceLink() {
		return idTriple;
	}
	@XmlElement
	public void setPlaceLink(String placeLink) {
		this.idTriple = placeLink;
	}
	@XmlElement
	public String getIdTriple() {
		return idTriple;
	}
	@XmlElement
	public void setIdTriple(String idTriple) {
		this.idTriple = idTriple;
	}
	@XmlElement
	public boolean isUpdateGeo() {
		return this.updateGeo;
	}
	@XmlElement
	public void setUpdateGeo(boolean existGeo) {
		this.updateGeo= existGeo;
	}
	@XmlElement
	public String getIdGeo() {
		return idGeo;
	}
	@XmlElement
	public void setIdGeo(String idGeo) {
		this.idGeo = idGeo;
	}
	
	
}
