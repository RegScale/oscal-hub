'use client';

import { useState } from 'react';
import { formatDistanceToNow } from 'date-fns';
import { MessageSquare, Reply, Pencil, Trash2, User } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Card, CardContent } from '@/components/ui/card';
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
  AlertDialogTrigger,
} from '@/components/ui/alert-dialog';
import { CommentForm } from './comment-form';
import type { LibraryComment } from '@/types/oscal';
import { cn } from '@/lib/utils';

interface CommentItemProps {
  comment: LibraryComment;
  currentUsername?: string;
  depth?: number;
  onReply: (parentCommentId: string, content: string) => Promise<void>;
  onEdit: (commentId: string, content: string) => Promise<void>;
  onDelete: (commentId: string) => Promise<void>;
}

function CommentItem({
  comment,
  currentUsername,
  depth = 0,
  onReply,
  onEdit,
  onDelete,
}: CommentItemProps) {
  const [showReplyForm, setShowReplyForm] = useState(false);
  const [showEditForm, setShowEditForm] = useState(false);
  const isOwner = currentUsername === comment.username;
  const maxDepth = 3; // Limit nesting depth for readability

  const handleReply = async (content: string) => {
    await onReply(comment.commentId, content);
    setShowReplyForm(false);
  };

  const handleEdit = async (content: string) => {
    await onEdit(comment.commentId, content);
    setShowEditForm(false);
  };

  const handleDelete = async () => {
    await onDelete(comment.commentId);
  };

  const formattedDate = formatDistanceToNow(new Date(comment.createdAt), {
    addSuffix: true,
  });

  return (
    <div className={cn('space-y-2', depth > 0 && 'ml-6 border-l-2 border-muted pl-4')}>
      <div className="flex items-start gap-3">
        <div className="flex-shrink-0 w-8 h-8 rounded-full bg-primary/10 flex items-center justify-center">
          <User className="h-4 w-4 text-primary" />
        </div>
        <div className="flex-1 min-w-0">
          <div className="flex items-center gap-2 flex-wrap">
            <span className="font-medium text-sm">
              {comment.userDisplayName || comment.username}
            </span>
            <span className="text-xs text-muted-foreground">{formattedDate}</span>
            {comment.isEdited && (
              <span className="text-xs text-muted-foreground">(edited)</span>
            )}
          </div>

          {showEditForm ? (
            <div className="mt-2">
              <CommentForm
                onSubmit={handleEdit}
                onCancel={() => setShowEditForm(false)}
                placeholder="Edit your comment..."
                submitLabel="Save"
                isReply
                initialContent={comment.content}
              />
            </div>
          ) : (
            <p className="text-sm mt-1 whitespace-pre-wrap">{comment.content}</p>
          )}

          {!showEditForm && (
            <div className="flex items-center gap-2 mt-2">
              {depth < maxDepth && (
                <Button
                  variant="ghost"
                  size="sm"
                  className="h-7 text-xs"
                  onClick={() => setShowReplyForm(!showReplyForm)}
                >
                  <Reply className="h-3 w-3 mr-1" />
                  Reply
                </Button>
              )}
              {isOwner && (
                <>
                  <Button
                    variant="ghost"
                    size="sm"
                    className="h-7 text-xs"
                    onClick={() => setShowEditForm(true)}
                  >
                    <Pencil className="h-3 w-3 mr-1" />
                    Edit
                  </Button>
                  <AlertDialog>
                    <AlertDialogTrigger asChild>
                      <Button
                        variant="ghost"
                        size="sm"
                        className="h-7 text-xs text-destructive hover:text-destructive"
                      >
                        <Trash2 className="h-3 w-3 mr-1" />
                        Delete
                      </Button>
                    </AlertDialogTrigger>
                    <AlertDialogContent>
                      <AlertDialogHeader>
                        <AlertDialogTitle>Delete Comment</AlertDialogTitle>
                        <AlertDialogDescription>
                          Are you sure you want to delete this comment? This action cannot be
                          undone.
                        </AlertDialogDescription>
                      </AlertDialogHeader>
                      <AlertDialogFooter>
                        <AlertDialogCancel>Cancel</AlertDialogCancel>
                        <AlertDialogAction onClick={handleDelete}>Delete</AlertDialogAction>
                      </AlertDialogFooter>
                    </AlertDialogContent>
                  </AlertDialog>
                </>
              )}
            </div>
          )}

          {showReplyForm && (
            <div className="mt-2">
              <CommentForm
                onSubmit={handleReply}
                onCancel={() => setShowReplyForm(false)}
                placeholder={`Reply to ${comment.userDisplayName || comment.username}...`}
                submitLabel="Reply"
                isReply
              />
            </div>
          )}
        </div>
      </div>

      {/* Render replies recursively */}
      {comment.replies && comment.replies.length > 0 && (
        <div className="space-y-3 mt-3">
          {comment.replies.map((reply) => (
            <CommentItem
              key={reply.commentId}
              comment={reply}
              currentUsername={currentUsername}
              depth={depth + 1}
              onReply={onReply}
              onEdit={onEdit}
              onDelete={onDelete}
            />
          ))}
        </div>
      )}
    </div>
  );
}

interface CommentThreadProps {
  comments: LibraryComment[];
  itemId: string;
  currentUsername?: string;
  onCommentAdded: () => void;
  onCreateComment: (content: string, parentCommentId?: string) => Promise<void>;
  onEditComment: (commentId: string, content: string) => Promise<void>;
  onDeleteComment: (commentId: string) => Promise<void>;
}

export function CommentThread({
  comments,
  itemId,
  currentUsername,
  onCommentAdded,
  onCreateComment,
  onEditComment,
  onDeleteComment,
}: CommentThreadProps) {
  const handleCreateComment = async (content: string) => {
    await onCreateComment(content);
    onCommentAdded();
  };

  const handleReply = async (parentCommentId: string, content: string) => {
    await onCreateComment(content, parentCommentId);
    onCommentAdded();
  };

  const handleEdit = async (commentId: string, content: string) => {
    await onEditComment(commentId, content);
    onCommentAdded();
  };

  const handleDelete = async (commentId: string) => {
    await onDeleteComment(commentId);
    onCommentAdded();
  };

  return (
    <div className="space-y-6">
      {/* New comment form */}
      <Card>
        <CardContent className="pt-4">
          <CommentForm
            onSubmit={handleCreateComment}
            placeholder="Add a comment..."
            submitLabel="Post Comment"
          />
        </CardContent>
      </Card>

      {/* Comments list */}
      {comments.length === 0 ? (
        <div className="text-center py-8 text-muted-foreground">
          <MessageSquare className="h-12 w-12 mx-auto mb-2 opacity-50" />
          <p>No comments yet. Be the first to comment!</p>
        </div>
      ) : (
        <div className="space-y-4">
          {comments.map((comment) => (
            <Card key={comment.commentId}>
              <CardContent className="pt-4">
                <CommentItem
                  comment={comment}
                  currentUsername={currentUsername}
                  onReply={handleReply}
                  onEdit={handleEdit}
                  onDelete={handleDelete}
                />
              </CardContent>
            </Card>
          ))}
        </div>
      )}
    </div>
  );
}
