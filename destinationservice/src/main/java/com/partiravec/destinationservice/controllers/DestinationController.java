package com.partiravec.destinationservice.controllers;

import com.partiravec.destinationservice.dao.CountryDao;
import com.partiravec.destinationservice.dao.DestinationDao;
import com.partiravec.destinationservice.models.Country;
import com.partiravec.destinationservice.models.Destination;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/dest")
public class DestinationController {

    private final DestinationDao destinationDao;
    private final CountryDao countryDao;
    private final BookController bookController;

    /**
     * Inject dependences
     * @param destinationDao dao for destinations
     * @param countryDao dao for countries
     * @param bookController controller for books
     */
    @Autowired
    public DestinationController(DestinationDao destinationDao, CountryDao countryDao, BookController bookController) {
        this.destinationDao = destinationDao;
        this.countryDao = countryDao;
        this.bookController = bookController;
    }

    @GetMapping("")
    public String getApi() {
        System.out.println("hello !");
        return "[DestinationService] Hello World";
    }

    /**
     * Get a destination by id
     * @param id destination id
     * @return The destination if it exists
     */
    @GetMapping("/destination/{id}")
    public Destination getDestinationById(@PathVariable("id") int id) {
        Optional<Destination> optionalDestination = destinationDao.findById(id);
        return optionalDestination.orElse(null);
    }

    @GetMapping("/destinations")
    public Iterable<Destination> getDestinations(
            @RequestParam(name = "offset", defaultValue = "-1") String s_offset,
            @RequestParam(name = "limit", defaultValue = "-1") String s_limit) {

        try {
            int offset = Integer.parseInt(s_offset);
            int limit = Integer.parseInt(s_limit);
            if (offset != -1 && limit != -1) {
                return destinationDao.findAll(PageRequest.of(offset, limit, Sort.by(Sort.Direction.DESC, "updateDate")));
            }
        } catch (NumberFormatException ignored) {  }

        return destinationDao.findAll();


    }

    /**
     * Get all destinations from a country code
     * @param countryCode code of the country where we want the destinations
     * @return list of destinations
     */
    @GetMapping("/destinations/country/{country}")
    public List<Destination> getDestinationByCountry(@PathVariable("country") String countryCode) {
        return destinationDao.findByCountryCode(countryCode);
    }

    /**
     * Get all countries with at least one destination
     * @return list of the countries
     */
    @GetMapping("/countries")
    public List<Country> getCountriesWithDestinations() {
        return countryDao.findAllCountriesHavingDestinations();
    }


    /**
     * Call saveDestination() to create a destination in database,
     * If the associate country does not exists, it creates it
     * @param destination destination to create
     */
    @PostMapping("/destination")
    public Destination createDestination(@RequestBody Destination destination, @RequestParam("_user") String userName) {
        // Controle : User can access the book
        if(bookController.userHasAccessToBook(userName, destination.getBook_id())) {
            // Set the author with current user
            destination.setAuthor(userName);

            // Save destination + country
            saveDestination(destination);
            return destination;
        }
        return null;

    }


    /**
     * Call saveDestination() to update a destination in database,
     * If the associate country does not exists, it creates it
     * @param destination destination to update
     */
    @PatchMapping("/destination")
    public Destination updateDestination(@RequestBody Destination destination, @RequestParam("_user") String userName) {
        // Controle : Current user has access to the destination book
        if(bookController.userHasAccessToBook(userName, destination.getBook_id())) {
            saveDestination(destination);
            return destination;
        }
        return null;
    }

    /**
     * Save a destination in database (create it if it doesn't exists or update it),
     * If the associate country does not exists, it creates it
     * @param destination destination to save
     */
    public void saveDestination(Destination destination) {
        // If the country does not exist, create it
        Optional<Country> optionalCountry = countryDao.findByCode(destination.getCountry().getCode());
        if (optionalCountry.isEmpty()) {
            // Create the country
            countryDao.save(destination.getCountry());
        } else {
            // Set the write value to the destination object
            destination.setCountry(optionalCountry.get());
        }

        // Save destination
        destinationDao.save(destination);
    }


}
