'use client';

import ProtectedRoute from '@/components/ProtectedRoute';
import { LeaderboardContent } from './leaderboard-content';

export default function LeaderboardPage() {
  return (
    <ProtectedRoute>
      <LeaderboardContent />
    </ProtectedRoute>
  );
}
