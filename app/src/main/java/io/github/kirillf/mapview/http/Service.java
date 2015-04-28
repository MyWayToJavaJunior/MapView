package io.github.kirillf.mapview.http;

/**
 * Remote service interface
 * @param <Req> request type
 * @param <Rep> response type
 */
public interface Service<Req, Rep> {
    Rep doRequest(Req req);
}
