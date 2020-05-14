package kr.puze.goomap;

public class MapData {
    public String image;
    public String title;
    public String number;
    public Double lat;
    public Double lng;

    public MapData() {
        // Default constructor required for calls to DataSnapshot.getValue(User.class)
    }

    public MapData(String image, String title, String number, Double lat, Double lng) {
        this.image = image;
        this.title = title;
        this.number = number;
        this.lat = lat;
        this.lng = lng;
    }
}