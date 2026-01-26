package com.example.WELLS.service;

import com.example.WELLS.model.Person;
import com.example.WELLS.repository.PersonRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PersonService {

    private final PersonRepository repo;

    public PersonService(PersonRepository repo) {
        this.repo = repo;
    }

    public Person save(Person person) {
        return repo.save(person);
    }

    public List<Person> findAll() {
        return repo.findAll();
    }

    public Person findById(Long id) {
        return repo.findById(id).orElse(null);
    }
}
