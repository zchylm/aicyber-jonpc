import { useEffect, useState } from "react";
import { heroSlides } from "../data/hero";

function HeroVisual() {
  const [activeIndex, setActiveIndex] = useState(0);
  const [isPaused, setIsPaused] = useState(false);
  const activeSlide = heroSlides[activeIndex];

  useEffect(() => {
    if (isPaused) return undefined;
    const timer = window.setInterval(() => {
      setActiveIndex((current) => (current + 1) % heroSlides.length);
    }, 6500);
    return () => window.clearInterval(timer);
  }, [isPaused]);

  function moveSlide(direction: -1 | 1) {
    setActiveIndex((current) => (current + direction + heroSlides.length) % heroSlides.length);
  }

  return (
    <div
      className="hero-carousel"
      aria-label="JON. PC systems by direction"
      onMouseEnter={() => setIsPaused(true)}
      onMouseLeave={() => setIsPaused(false)}
      onFocus={() => setIsPaused(true)}
      onBlur={(event) => { if (!event.currentTarget.contains(event.relatedTarget)) setIsPaused(false); }}
    >
      <div className="hero-carousel-image">
        <img key={activeSlide.id} src={activeSlide.image} alt={activeSlide.alt} />
      </div>
      <div className="hero-carousel-overlay" aria-hidden="true" />
      <div className="hero-carousel-content" aria-live="polite">
        <span className="hero-slide-kicker">{activeSlide.kicker}</span>
        <h1 id="hero-title">{activeSlide.title}</h1>
        <p>{activeSlide.description}</p>
        <a className="button button-primary" href="#build">Find your system <span aria-hidden="true">↗</span></a>
      </div>
      <div className="hero-carousel-controls">
        <div className="hero-carousel-counter"><strong>0{activeIndex + 1}</strong><span>/ 05</span></div>
        <div className="hero-carousel-dots" role="tablist" aria-label="Choose hero slide">
          {heroSlides.map((slide, index) => (
            <button className={index === activeIndex ? "hero-carousel-dot hero-carousel-dot-active" : "hero-carousel-dot"} type="button" role="tab" aria-selected={index === activeIndex} aria-label={`Show ${slide.kicker}`} key={slide.id} onClick={() => setActiveIndex(index)}>
              <span />
            </button>
          ))}
        </div>
        <div className="hero-carousel-arrows">
          <button type="button" onClick={() => moveSlide(-1)} aria-label="Previous slide">←</button>
          <button type="button" onClick={() => moveSlide(1)} aria-label="Next slide">→</button>
        </div>
      </div>
    </div>
  );
}

export default HeroVisual;
