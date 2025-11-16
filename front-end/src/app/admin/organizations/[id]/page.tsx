'use client';

import { useParams, useRouter } from 'next/navigation';
import { useEffect, useState, useRef } from 'react';

interface Organization {
  id: number;
  name: string;
  description: string | null;
  logoUrl: string | null;
  active: boolean;
  createdAt: string;
  memberCount: number;
}

interface Member {
  membershipId: number;
  userId: number;
  username: string;
  email: string;
  role: string;
  status: string;
  joinedAt: string;
}

interface User {
  id: number;
  username: string;
  email: string;
}

export default function ManageOrganizationPage() {
  const params = useParams();
  const router = useRouter();
  const organizationId = params.id as string;
  const fileInputRef = useRef<HTMLInputElement>(null);

  const [organization, setOrganization] = useState<Organization | null>(null);
  const [members, setMembers] = useState<Member[]>([]);
  const [allUsers, setAllUsers] = useState<User[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);

  // Edit mode state
  const [editMode, setEditMode] = useState(false);
  const [editName, setEditName] = useState('');
  const [editDescription, setEditDescription] = useState('');
  const [editActive, setEditActive] = useState(true);
  const [saving, setSaving] = useState(false);

  // Logo upload state
  const [uploadingLogo, setUploadingLogo] = useState(false);
  const [deletingLogo, setDeletingLogo] = useState(false);

  // Members tab state
  const [activeTab, setActiveTab] = useState<'details' | 'members'>('details');

  // Add member state
  const [showAddMemberModal, setShowAddMemberModal] = useState(false);
  const [newMemberUserId, setNewMemberUserId] = useState<string>('');
  const [newMemberRole, setNewMemberRole] = useState<'USER' | 'ORG_ADMIN'>('USER');
  const [addingMember, setAddingMember] = useState(false);

  // Edit member role state
  const [editingMembershipId, setEditingMembershipId] = useState<number | null>(null);
  const [editingMemberRole, setEditingMemberRole] = useState<string>('');

  // Remove member state
  const [removingMembershipId, setRemovingMembershipId] = useState<number | null>(null);

  useEffect(() => {
    loadOrganization();
    loadMembers();
    loadAllUsers();
  }, [organizationId]);

  const loadOrganization = async () => {
    try {
      setLoading(true);
      setError(null);
      const response = await fetch(`http://localhost:8080/api/admin/organizations/${organizationId}`, {
        headers: {
          'Authorization': `Bearer ${localStorage.getItem('token')}`,
        },
      });

      if (!response.ok) {
        throw new Error('Failed to load organization');
      }

      const org = await response.json();
      setOrganization(org);
      setEditName(org.name);
      setEditDescription(org.description || '');
      setEditActive(org.active);
    } catch (err: any) {
      console.error('Failed to load organization:', err);
      setError(err.message || 'Failed to load organization');
    } finally {
      setLoading(false);
    }
  };

  const loadMembers = async () => {
    try {
      const response = await fetch(`http://localhost:8080/api/admin/organizations/${organizationId}/members`, {
        headers: {
          'Authorization': `Bearer ${localStorage.getItem('token')}`,
        },
      });

      if (response.ok) {
        const data = await response.json();
        setMembers(data);
      }
    } catch (err) {
      console.error('Failed to load members:', err);
    }
  };

  const loadAllUsers = async () => {
    try {
      const response = await fetch(`http://localhost:8080/api/admin/users`, {
        headers: {
          'Authorization': `Bearer ${localStorage.getItem('token')}`,
        },
      });

      if (response.ok) {
        const data = await response.json();
        setAllUsers(data);
      }
    } catch (err) {
      console.error('Failed to load users:', err);
    }
  };

  const handleLogoUpload = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    // Validate file type
    if (!['image/png', 'image/jpeg', 'image/jpg', 'image/svg+xml'].includes(file.type)) {
      setError('Invalid file type. Please upload PNG, JPG, or SVG');
      return;
    }

    // Validate file size (2MB max)
    if (file.size > 2 * 1024 * 1024) {
      setError('File size must be less than 2MB');
      return;
    }

    try {
      setUploadingLogo(true);
      setError(null);
      setSuccess(null);

      const formData = new FormData();
      formData.append('file', file);

      const response = await fetch(`http://localhost:8080/api/admin/organizations/${organizationId}/logo`, {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${localStorage.getItem('token')}`,
        },
        body: formData,
      });

      if (!response.ok) {
        const error = await response.json();
        throw new Error(error.error || 'Failed to upload logo');
      }

      await loadOrganization();
      setSuccess('Logo uploaded successfully');
      setTimeout(() => setSuccess(null), 3000);
    } catch (err: any) {
      console.error('Failed to upload logo:', err);
      setError(err.message || 'Failed to upload logo');
    } finally {
      setUploadingLogo(false);
      if (fileInputRef.current) {
        fileInputRef.current.value = '';
      }
    }
  };

  const handleDeleteLogo = async () => {
    if (!confirm('Are you sure you want to delete the logo?')) return;

    try {
      setDeletingLogo(true);
      setError(null);
      setSuccess(null);

      const response = await fetch(`http://localhost:8080/api/admin/organizations/${organizationId}/logo`, {
        method: 'DELETE',
        headers: {
          'Authorization': `Bearer ${localStorage.getItem('token')}`,
        },
      });

      if (!response.ok) {
        const error = await response.json();
        throw new Error(error.error || 'Failed to delete logo');
      }

      await loadOrganization();
      setSuccess('Logo deleted successfully');
      setTimeout(() => setSuccess(null), 3000);
    } catch (err: any) {
      console.error('Failed to delete logo:', err);
      setError(err.message || 'Failed to delete logo');
    } finally {
      setDeletingLogo(false);
    }
  };

  const handleAddMember = async () => {
    if (!newMemberUserId) {
      setError('Please select a user');
      return;
    }

    try {
      setAddingMember(true);
      setError(null);
      setSuccess(null);

      const response = await fetch(`http://localhost:8080/api/admin/organizations/${organizationId}/members`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${localStorage.getItem('token')}`,
        },
        body: JSON.stringify({
          userId: parseInt(newMemberUserId),
          role: newMemberRole,
        }),
      });

      if (!response.ok) {
        const error = await response.json();
        throw new Error(error.error || 'Failed to add member');
      }

      setShowAddMemberModal(false);
      setNewMemberUserId('');
      setNewMemberRole('USER');
      await loadMembers();
      setSuccess('Member added successfully');
      setTimeout(() => setSuccess(null), 3000);
    } catch (err: any) {
      console.error('Failed to add member:', err);
      setError(err.message || 'Failed to add member');
    } finally {
      setAddingMember(false);
    }
  };

  const handleUpdateMemberRole = async (membershipId: number, newRole: string) => {
    try {
      setError(null);
      setSuccess(null);

      const response = await fetch(`http://localhost:8080/api/admin/organizations/${organizationId}/members/${membershipId}`, {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${localStorage.getItem('token')}`,
        },
        body: JSON.stringify({ role: newRole }),
      });

      if (!response.ok) {
        const error = await response.json();
        throw new Error(error.error || 'Failed to update member role');
      }

      setEditingMembershipId(null);
      await loadMembers();
      setSuccess('Member role updated successfully');
      setTimeout(() => setSuccess(null), 3000);
    } catch (err: any) {
      console.error('Failed to update member role:', err);
      setError(err.message || 'Failed to update member role');
    }
  };

  const handleRemoveMember = async (membershipId: number) => {
    if (!confirm('Are you sure you want to remove this member from the organization?')) return;

    try {
      setRemovingMembershipId(membershipId);
      setError(null);
      setSuccess(null);

      const response = await fetch(`http://localhost:8080/api/admin/organizations/${organizationId}/members/${membershipId}`, {
        method: 'DELETE',
        headers: {
          'Authorization': `Bearer ${localStorage.getItem('token')}`,
        },
      });

      if (!response.ok) {
        const error = await response.json();
        throw new Error(error.error || 'Failed to remove member');
      }

      await loadMembers();
      setSuccess('Member removed successfully');
      setTimeout(() => setSuccess(null), 3000);
    } catch (err: any) {
      console.error('Failed to remove member:', err);
      setError(err.message || 'Failed to remove member');
    } finally {
      setRemovingMembershipId(null);
    }
  };

  const handleSaveChanges = async () => {
    if (!editName.trim()) {
      setError('Organization name is required');
      return;
    }

    try {
      setSaving(true);
      setError(null);
      setSuccess(null);

      const response = await fetch(`http://localhost:8080/api/admin/organizations/${organizationId}`, {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${localStorage.getItem('token')}`,
        },
        body: JSON.stringify({
          name: editName.trim(),
          description: editDescription.trim() || null,
          active: editActive,
        }),
      });

      if (!response.ok) {
        const error = await response.json();
        throw new Error(error.error || 'Failed to update organization');
      }

      const updated = await response.json();
      setOrganization(updated);
      setEditMode(false);
      setSuccess('Organization updated successfully');
      setTimeout(() => setSuccess(null), 3000);
    } catch (err: any) {
      console.error('Failed to update organization:', err);
      setError(err.message || 'Failed to update organization');
    } finally {
      setSaving(false);
    }
  };

  const handleCancelEdit = () => {
    if (organization) {
      setEditName(organization.name);
      setEditDescription(organization.description || '');
      setEditActive(organization.active);
    }
    setEditMode(false);
    setError(null);
  };

  const handleToggleActive = async () => {
    if (!organization) return;

    const confirmed = confirm(
      organization.active
        ? 'Are you sure you want to deactivate this organization? Users will lose access.'
        : 'Are you sure you want to reactivate this organization?'
    );

    if (!confirmed) return;

    try {
      setSaving(true);
      setError(null);
      setSuccess(null);

      if (organization.active) {
        // Deactivate
        const response = await fetch(`http://localhost:8080/api/admin/organizations/${organizationId}`, {
          method: 'DELETE',
          headers: {
            'Authorization': `Bearer ${localStorage.getItem('token')}`,
          },
        });

        if (!response.ok) {
          throw new Error('Failed to deactivate organization');
        }
      } else {
        // Reactivate
        const response = await fetch(`http://localhost:8080/api/admin/organizations/${organizationId}`, {
          method: 'PUT',
          headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${localStorage.getItem('token')}`,
          },
          body: JSON.stringify({
            name: organization.name,
            description: organization.description,
            active: true,
          }),
        });

        if (!response.ok) {
          throw new Error('Failed to reactivate organization');
        }
      }

      await loadOrganization();
      setSuccess(organization.active ? 'Organization deactivated successfully' : 'Organization reactivated successfully');
      setTimeout(() => setSuccess(null), 3000);
    } catch (err: any) {
      console.error('Failed to toggle organization status:', err);
      setError(err.message || 'Failed to update organization status');
    } finally {
      setSaving(false);
    }
  };

  if (loading) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <div className="text-center">
          <div className="animate-spin rounded-full h-16 w-16 border-b-2 border-blue-600 mx-auto"></div>
          <p className="mt-4 text-gray-600 dark:text-gray-400">Loading organization...</p>
        </div>
      </div>
    );
  }

  if (error && !organization) {
    return (
      <div className="min-h-screen bg-gray-50 dark:bg-gray-900 py-8 px-4 sm:px-6 lg:px-8">
        <div className="max-w-4xl mx-auto">
          <div className="bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800 rounded-lg p-4">
            <p className="text-red-800 dark:text-red-200">{error}</p>
          </div>
          <button
            onClick={() => router.push('/admin/organizations')}
            className="mt-4 px-4 py-2 bg-gray-600 text-white rounded-md hover:bg-gray-700"
          >
            Back to Organizations
          </button>
        </div>
      </div>
    );
  }

  if (!organization) {
    return (
      <div className="min-h-screen bg-gray-50 dark:bg-gray-900 py-8 px-4 sm:px-6 lg:px-8">
        <div className="max-w-4xl mx-auto">
          <p className="text-gray-600 dark:text-gray-400">Organization not found</p>
          <button
            onClick={() => router.push('/admin/organizations')}
            className="mt-4 px-4 py-2 bg-gray-600 text-white rounded-md hover:bg-gray-700"
          >
            Back to Organizations
          </button>
        </div>
      </div>
    );
  }

  // Filter out users who are already members
  const availableUsers = allUsers.filter(user =>
    !members.some(member => member.userId === user.id)
  );

  return (
    <div className="min-h-screen bg-gray-50 dark:bg-gray-900 py-8 px-4 sm:px-6 lg:px-8">
      <div className="max-w-6xl mx-auto">
        {/* Header */}
        <div className="mb-6">
          <button
            onClick={() => router.push('/admin/organizations')}
            className="text-blue-600 hover:text-blue-700 dark:text-blue-400 dark:hover:text-blue-300 flex items-center"
          >
            <svg className="h-5 w-5 mr-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 19l-7-7 7-7" />
            </svg>
            Back to Organizations
          </button>
        </div>

        {/* Success Message */}
        {success && (
          <div className="mb-6 bg-green-50 dark:bg-green-900/20 border border-green-200 dark:border-green-800 rounded-lg p-4">
            <p className="text-green-800 dark:text-green-200">{success}</p>
          </div>
        )}

        {/* Error Message */}
        {error && (
          <div className="mb-6 bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800 rounded-lg p-4">
            <p className="text-red-800 dark:text-red-200">{error}</p>
          </div>
        )}

        {/* Tabs */}
        <div className="mb-6 border-b border-gray-200 dark:border-gray-700">
          <nav className="-mb-px flex space-x-8">
            <button
              onClick={() => setActiveTab('details')}
              className={`py-4 px-1 border-b-2 font-medium text-sm ${
                activeTab === 'details'
                  ? 'border-blue-500 text-blue-600 dark:text-blue-400'
                  : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300 dark:text-gray-400'
              }`}
            >
              Details
            </button>
            <button
              onClick={() => setActiveTab('members')}
              className={`py-4 px-1 border-b-2 font-medium text-sm ${
                activeTab === 'members'
                  ? 'border-blue-500 text-blue-600 dark:text-blue-400'
                  : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300 dark:text-gray-400'
              }`}
            >
              Members ({members.length})
            </button>
          </nav>
        </div>

        {/* Details Tab */}
        {activeTab === 'details' && (
          <div className="bg-white dark:bg-gray-800 rounded-lg shadow p-6">
            <div className="flex items-center justify-between mb-6">
              <div className="flex items-center">
                {organization.logoUrl ? (
                  <img src={`http://localhost:8080${organization.logoUrl}`} alt={organization.name} className="h-16 w-16 rounded object-contain mr-4" />
                ) : (
                  <div className="h-16 w-16 rounded bg-gradient-to-br from-blue-500 to-indigo-600 flex items-center justify-center mr-4">
                    <span className="text-white text-2xl font-bold">{organization.name.charAt(0).toUpperCase()}</span>
                  </div>
                )}
                <div>
                  <h1 className="text-2xl font-bold text-gray-900 dark:text-white">{organization.name}</h1>
                  <span className={`px-2 inline-flex text-xs leading-5 font-semibold rounded-full ${
                    organization.active
                      ? 'bg-green-100 text-green-800 dark:bg-green-900 dark:text-green-200'
                      : 'bg-red-100 text-red-800 dark:bg-red-900 dark:text-red-200'
                  }`}>
                    {organization.active ? 'Active' : 'Inactive'}
                  </span>
                </div>
              </div>
              <div className="flex space-x-2">
                {!editMode && (
                  <>
                    <button
                      onClick={() => setEditMode(true)}
                      className="px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700"
                    >
                      Edit
                    </button>
                    <button
                      onClick={handleToggleActive}
                      disabled={saving}
                      className={`px-4 py-2 rounded-md ${
                        organization.active
                          ? 'bg-red-600 hover:bg-red-700 text-white'
                          : 'bg-green-600 hover:bg-green-700 text-white'
                      } disabled:opacity-50`}
                    >
                      {organization.active ? 'Deactivate' : 'Reactivate'}
                    </button>
                  </>
                )}
              </div>
            </div>

            {editMode ? (
              /* Edit Form */
              <div className="space-y-4">
                <div>
                  <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                    Organization Name *
                  </label>
                  <input
                    type="text"
                    value={editName}
                    onChange={(e) => setEditName(e.target.value)}
                    className="w-full px-4 py-2 border border-gray-300 dark:border-gray-700 rounded-md focus:ring-2 focus:ring-blue-500 focus:border-transparent bg-white dark:bg-gray-900 text-gray-900 dark:text-white"
                    placeholder="Organization name"
                  />
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                    Description
                  </label>
                  <textarea
                    value={editDescription}
                    onChange={(e) => setEditDescription(e.target.value)}
                    rows={3}
                    className="w-full px-4 py-2 border border-gray-300 dark:border-gray-700 rounded-md focus:ring-2 focus:ring-blue-500 focus:border-transparent bg-white dark:bg-gray-900 text-gray-900 dark:text-white"
                    placeholder="Brief description of the organization"
                  />
                </div>

                <div className="flex items-center">
                  <input
                    type="checkbox"
                    id="active"
                    checked={editActive}
                    onChange={(e) => setEditActive(e.target.checked)}
                    className="h-4 w-4 text-blue-600 focus:ring-blue-500 border-gray-300 rounded"
                  />
                  <label htmlFor="active" className="ml-2 block text-sm text-gray-900 dark:text-white">
                    Active
                  </label>
                </div>

                {/* Logo Management */}
                <div className="pt-4 border-t border-gray-200 dark:border-gray-700">
                  <h3 className="text-sm font-medium text-gray-700 dark:text-gray-300 mb-3">Organization Logo</h3>
                  <div className="flex items-center space-x-4">
                    <input
                      ref={fileInputRef}
                      type="file"
                      accept="image/png,image/jpeg,image/jpg,image/svg+xml"
                      onChange={handleLogoUpload}
                      className="hidden"
                    />
                    <button
                      type="button"
                      onClick={() => fileInputRef.current?.click()}
                      disabled={uploadingLogo}
                      className="px-4 py-2 border border-gray-300 dark:border-gray-700 text-gray-700 dark:text-gray-300 rounded-md hover:bg-gray-50 dark:hover:bg-gray-800 disabled:opacity-50"
                    >
                      {uploadingLogo ? 'Uploading...' : organization.logoUrl ? 'Change Logo' : 'Upload Logo'}
                    </button>
                    {organization.logoUrl && (
                      <button
                        type="button"
                        onClick={handleDeleteLogo}
                        disabled={deletingLogo}
                        className="px-4 py-2 border border-red-300 dark:border-red-700 text-red-600 dark:text-red-400 rounded-md hover:bg-red-50 dark:hover:bg-red-900/20 disabled:opacity-50"
                      >
                        {deletingLogo ? 'Deleting...' : 'Delete Logo'}
                      </button>
                    )}
                    <span className="text-sm text-gray-500 dark:text-gray-400">PNG, JPG, or SVG (max 2MB)</span>
                  </div>
                </div>

                <div className="flex justify-end space-x-3 pt-4">
                  <button
                    type="button"
                    onClick={handleCancelEdit}
                    disabled={saving}
                    className="px-4 py-2 border border-gray-300 dark:border-gray-700 text-gray-700 dark:text-gray-300 rounded-md hover:bg-gray-50 dark:hover:bg-gray-800 disabled:opacity-50"
                  >
                    Cancel
                  </button>
                  <button
                    type="button"
                    onClick={handleSaveChanges}
                    disabled={saving}
                    className="px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700 disabled:opacity-50"
                  >
                    {saving ? 'Saving...' : 'Save Changes'}
                  </button>
                </div>
              </div>
            ) : (
              /* View Mode */
              <div className="border-t border-gray-200 dark:border-gray-700 pt-6">
                <dl className="grid grid-cols-1 gap-4 sm:grid-cols-2">
                  <div>
                    <dt className="text-sm font-medium text-gray-500 dark:text-gray-400">Description</dt>
                    <dd className="mt-1 text-sm text-gray-900 dark:text-white">
                      {organization.description || 'No description'}
                    </dd>
                  </div>
                  <div>
                    <dt className="text-sm font-medium text-gray-500 dark:text-gray-400">Created</dt>
                    <dd className="mt-1 text-sm text-gray-900 dark:text-white">
                      {new Date(organization.createdAt).toLocaleString()}
                    </dd>
                  </div>
                  <div>
                    <dt className="text-sm font-medium text-gray-500 dark:text-gray-400">Organization ID</dt>
                    <dd className="mt-1 text-sm text-gray-900 dark:text-white">{organization.id}</dd>
                  </div>
                  <div>
                    <dt className="text-sm font-medium text-gray-500 dark:text-gray-400">Members</dt>
                    <dd className="mt-1 text-sm text-gray-900 dark:text-white">{members.length}</dd>
                  </div>
                </dl>
              </div>
            )}
          </div>
        )}

        {/* Members Tab */}
        {activeTab === 'members' && (
          <div className="bg-white dark:bg-gray-800 rounded-lg shadow overflow-hidden">
            <div className="px-6 py-4 border-b border-gray-200 dark:border-gray-700 flex justify-between items-center">
              <h2 className="text-lg font-semibold text-gray-900 dark:text-white">Organization Members</h2>
              <button
                onClick={() => setShowAddMemberModal(true)}
                className="px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700"
              >
                Add Member
              </button>
            </div>
            <div className="overflow-x-auto">
              <table className="min-w-full divide-y divide-gray-200 dark:divide-gray-700">
                <thead className="bg-gray-50 dark:bg-gray-700">
                  <tr>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-300 uppercase tracking-wider">
                      User
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-300 uppercase tracking-wider">
                      Email
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-300 uppercase tracking-wider">
                      Role
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-300 uppercase tracking-wider">
                      Status
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-300 uppercase tracking-wider">
                      Joined
                    </th>
                    <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 dark:text-gray-300 uppercase tracking-wider">
                      Actions
                    </th>
                  </tr>
                </thead>
                <tbody className="bg-white dark:bg-gray-800 divide-y divide-gray-200 dark:divide-gray-700">
                  {members.length === 0 ? (
                    <tr>
                      <td colSpan={6} className="px-6 py-12 text-center text-gray-500 dark:text-gray-400">
                        No members found. Click "Add Member" to get started.
                      </td>
                    </tr>
                  ) : (
                    members.map((member) => (
                      <tr key={member.membershipId} className="hover:bg-gray-50 dark:hover:bg-gray-700">
                        <td className="px-6 py-4 whitespace-nowrap">
                          <div className="text-sm font-medium text-gray-900 dark:text-white">
                            {member.username}
                          </div>
                        </td>
                        <td className="px-6 py-4 whitespace-nowrap">
                          <div className="text-sm text-gray-500 dark:text-gray-400">{member.email}</div>
                        </td>
                        <td className="px-6 py-4 whitespace-nowrap">
                          {editingMembershipId === member.membershipId ? (
                            <select
                              value={editingMemberRole}
                              onChange={(e) => setEditingMemberRole(e.target.value)}
                              className="text-sm border border-gray-300 dark:border-gray-700 rounded px-2 py-1 bg-white dark:bg-gray-900 text-gray-900 dark:text-white"
                            >
                              <option value="USER">User</option>
                              <option value="ORG_ADMIN">Admin</option>
                            </select>
                          ) : (
                            <span className={`px-2 inline-flex text-xs leading-5 font-semibold rounded-full ${
                              member.role === 'ORG_ADMIN'
                                ? 'bg-purple-100 text-purple-800 dark:bg-purple-900 dark:text-purple-200'
                                : 'bg-gray-100 text-gray-800 dark:bg-gray-700 dark:text-gray-200'
                            }`}>
                              {member.role === 'ORG_ADMIN' ? 'Admin' : 'User'}
                            </span>
                          )}
                        </td>
                        <td className="px-6 py-4 whitespace-nowrap">
                          <span className={`px-2 inline-flex text-xs leading-5 font-semibold rounded-full ${
                            member.status === 'ACTIVE'
                              ? 'bg-green-100 text-green-800 dark:bg-green-900 dark:text-green-200'
                              : member.status === 'LOCKED'
                              ? 'bg-yellow-100 text-yellow-800 dark:bg-yellow-900 dark:text-yellow-200'
                              : 'bg-red-100 text-red-800 dark:bg-red-900 dark:text-red-200'
                          }`}>
                            {member.status}
                          </span>
                        </td>
                        <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500 dark:text-gray-400">
                          {new Date(member.joinedAt).toLocaleDateString()}
                        </td>
                        <td className="px-6 py-4 whitespace-nowrap text-right text-sm font-medium">
                          {editingMembershipId === member.membershipId ? (
                            <>
                              <button
                                onClick={() => handleUpdateMemberRole(member.membershipId, editingMemberRole)}
                                className="text-green-600 hover:text-green-900 dark:text-green-400 dark:hover:text-green-300 mr-4"
                              >
                                Save
                              </button>
                              <button
                                onClick={() => setEditingMembershipId(null)}
                                className="text-gray-600 hover:text-gray-900 dark:text-gray-400 dark:hover:text-gray-300"
                              >
                                Cancel
                              </button>
                            </>
                          ) : (
                            <>
                              <button
                                onClick={() => {
                                  setEditingMembershipId(member.membershipId);
                                  setEditingMemberRole(member.role);
                                }}
                                className="text-blue-600 hover:text-blue-900 dark:text-blue-400 dark:hover:text-blue-300 mr-4"
                              >
                                Edit Role
                              </button>
                              <button
                                onClick={() => handleRemoveMember(member.membershipId)}
                                disabled={removingMembershipId === member.membershipId}
                                className="text-red-600 hover:text-red-900 dark:text-red-400 dark:hover:text-red-300 disabled:opacity-50"
                              >
                                {removingMembershipId === member.membershipId ? 'Removing...' : 'Remove'}
                              </button>
                            </>
                          )}
                        </td>
                      </tr>
                    ))
                  )}
                </tbody>
              </table>
            </div>
          </div>
        )}
      </div>

      {/* Add Member Modal */}
      {showAddMemberModal && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
          <div className="bg-white dark:bg-gray-800 rounded-lg shadow-xl max-w-lg w-full mx-4">
            <div className="px-6 py-4 border-b border-gray-200 dark:border-gray-700">
              <h2 className="text-xl font-semibold text-gray-900 dark:text-white">
                Add Member to Organization
              </h2>
            </div>
            <div className="px-6 py-4">
              <div className="space-y-4">
                <div>
                  <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                    Select User *
                  </label>
                  <select
                    value={newMemberUserId}
                    onChange={(e) => setNewMemberUserId(e.target.value)}
                    className="w-full px-4 py-2 border border-gray-300 dark:border-gray-700 rounded-md focus:ring-2 focus:ring-blue-500 focus:border-transparent bg-white dark:bg-gray-900 text-gray-900 dark:text-white"
                  >
                    <option value="">-- Select a user --</option>
                    {availableUsers.map((user) => (
                      <option key={user.id} value={user.id}>
                        {user.username} ({user.email})
                      </option>
                    ))}
                  </select>
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                    Role *
                  </label>
                  <select
                    value={newMemberRole}
                    onChange={(e) => setNewMemberRole(e.target.value as 'USER' | 'ORG_ADMIN')}
                    className="w-full px-4 py-2 border border-gray-300 dark:border-gray-700 rounded-md focus:ring-2 focus:ring-blue-500 focus:border-transparent bg-white dark:bg-gray-900 text-gray-900 dark:text-white"
                  >
                    <option value="USER">User</option>
                    <option value="ORG_ADMIN">Admin</option>
                  </select>
                </div>
              </div>
            </div>
            <div className="px-6 py-4 border-t border-gray-200 dark:border-gray-700 flex justify-end space-x-3">
              <button
                onClick={() => {
                  setShowAddMemberModal(false);
                  setNewMemberUserId('');
                  setNewMemberRole('USER');
                }}
                disabled={addingMember}
                className="px-4 py-2 border border-gray-300 dark:border-gray-700 text-gray-700 dark:text-gray-300 rounded-md hover:bg-gray-50 dark:hover:bg-gray-800 disabled:opacity-50"
              >
                Cancel
              </button>
              <button
                onClick={handleAddMember}
                disabled={addingMember || !newMemberUserId}
                className="px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700 disabled:opacity-50"
              >
                {addingMember ? 'Adding...' : 'Add Member'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
