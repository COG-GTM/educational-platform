interface StarRatingProps {
  rating: number;
}

export default function StarRating({ rating }: StarRatingProps) {
  const rounded = Math.round(rating * 2) / 2;
  const stars = Array.from({ length: 5 }, (_, i) => {
    if (rounded >= i + 1) return 'full';
    if (rounded >= i + 0.5) return 'half';
    return 'empty';
  });

  return (
    <span className="star-rating" aria-label={`Rated ${rating.toFixed(1)} out of 5`}>
      {stars.map((kind, i) => (
        <span key={i} className={`star star-${kind}`} aria-hidden="true">
          {kind === 'empty' ? '☆' : '★'}
        </span>
      ))}
      <span className="star-rating-value">{rating.toFixed(1)}</span>
    </span>
  );
}
