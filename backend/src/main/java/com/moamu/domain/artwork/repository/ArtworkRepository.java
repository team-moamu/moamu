package com.moamu.domain.artwork.repository;

import java.util.Optional;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.moamu.domain.artwork.entity.Artwork;

/**
 * ArtworkRepository
 */
public interface ArtworkRepository extends JpaRepository<Artwork, Long> {

    Optional<Artwork> findByArtworkIdAndDelYn(Long artworkId, String delYn);

    @Query("""
        SELECT a FROM Artwork a
        WHERE a.status = com.moamu.domain.artwork.enums.Status.READY AND a.delYn = 'N'
        """)
    List<Artwork> findReadyArtworks();
}