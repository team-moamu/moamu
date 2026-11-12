package com.moamu.domain.artwork.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.moamu.domain.artwork.entity.Artwork;

/**
 * ArtworkRepository
 */
public interface ArtworkRepository extends JpaRepository<Artwork, Long> {
    Optional<Artwork> findByArtworkIdAndDelYn(Long artworkId, String delYn);
}