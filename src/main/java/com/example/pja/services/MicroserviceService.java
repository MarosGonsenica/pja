package com.example.pja.services;

import com.example.pja.controllers.data.Microservice;
import com.example.pja.repositories.MicroserviceRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

@Service
public class MicroserviceService {

    private final MicroserviceRepository microserviceRepository;

    private final RestTemplate restTemplate = new RestTemplate();

    public MicroserviceService(MicroserviceRepository microserviceRepository) {
        this.microserviceRepository = microserviceRepository;
    }

    public Microservice createPost(Microservice post) {
        // validuje userId pomocou externého API
        final String validateUserUrl = "https://jsonplaceholder.typicode.com/users/" + post.getUserId();
        try {
            restTemplate.getForObject(validateUserUrl, Object.class);
            // ak je userId platný, uloží príspevok
            return microserviceRepository.save(post);
        } catch (HttpClientErrorException.NotFound e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "userId not found with external API", e);
        }
    }


    public List<Microservice> getPostsByUserId(Integer userId) {
        return microserviceRepository.findByUserId(userId);
    }

    public Microservice getOrCreatePostById(Integer id) {
        Optional<Microservice> existingPost = microserviceRepository.findById(id);

        if (existingPost.isPresent()) {
            return existingPost.get();
        } else {
            Microservice post = findPostByIdExternalAPI(id);
            if (post != null) {
                return createPost(post);
            } else {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Post with Id "+ id + " not found");
            }
        }
    }

    public Microservice findPostByIdExternalAPI(Integer id) {
        final String validateIdUrl = "https://jsonplaceholder.typicode.com/posts/" + id;
        try {
            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<Microservice> response = restTemplate.getForEntity(validateIdUrl, Microservice.class);
            if (response.getStatusCode() == HttpStatus.OK) {
                return response.getBody();
            }
        } catch (HttpClientErrorException.NotFound e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Post with Id "+ id + " not found with external API", e);
        }
        return null;
    }

    public void deletePostById(Integer id) {
        try {
            microserviceRepository.deleteById(id);
        }
        catch (EmptyResultDataAccessException e){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Post with Id " + id + " not found", e);
        }
    }

    public Microservice updatePost(Integer id, String title, String body) {
        Microservice post = microserviceRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Post with Id "+ id + " not found"));

        post.setTitle(title);
        post.setBody(body);
        return microserviceRepository.save(post);
    }

}