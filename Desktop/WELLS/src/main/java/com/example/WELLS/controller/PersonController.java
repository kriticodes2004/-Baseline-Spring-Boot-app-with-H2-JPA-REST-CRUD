package com.example.WELLS.controller;

import com.example.WELLS.model.Person;
import com.example.WELLS.service.PersonService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/persons")
public class PersonController {

    private final PersonService service;

    public PersonController(PersonService service) {
        this.service = service;
    }

    // INSERT
    @PostMapping
    public Person create(@RequestBody Person person) {
        return service.save(person);
    }

    // FETCH ALL
    @GetMapping
    public List<Person> getAll() {
        return service.findAll();
    }

    // FETCH BY ID
    @GetMapping("/{id}")
    public Person getById(@PathVariable Long id) {
        return service.findById(id);
    }
}
