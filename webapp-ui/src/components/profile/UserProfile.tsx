import React, { useState } from 'react'
import { useAuth } from '../../contexts/AuthContext'
import { User } from '../../types'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '../ui/card'
import { Button } from '../ui/button'
import { Input } from '../ui/input'
import { Label } from '../ui/label'
import { Badge } from '../ui/badge'
import { Tabs, TabsContent, TabsList, TabsTrigger } from '../ui/tabs'
import { Avatar, AvatarFallback } from '../ui/avatar'
import { Switch } from '../ui/switch'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '../ui/select'
import { 
  Bell, 
  Save,
  Calendar,
  Clock
} from 'lucide-react'
import { apiClient } from '../../utils/api'
import { toast } from 'sonner'

const UserProfile: React.FC = () => {
  const { user, updateUser } = useAuth()
  const [loading, setLoading] = useState(false)
  const [profileData, setProfileData] = useState({
    firstName: user?.firstName || '',
    lastName: user?.lastName || '',
    email: user?.email || '',
  })
  const [notificationSettings, setNotificationSettings] = useState({
    dailyReports: true,
    weeklyReports: true,
    meetingReminders: true,
    teamUpdates: true,
  })
  const [reportSchedule, setReportSchedule] = useState({
    dailyTime: '18:00',
    weeklyDay: 'friday',
    weeklyTime: '17:00',
  })

  const handleProfileUpdate = async (e: React.FormEvent) => {
    e.preventDefault()
    setLoading(true)

    try {
      const updatedUser = await apiClient.put<User>('/api/users/me', profileData)
      updateUser(updatedUser)
      toast.success('Profile updated successfully!')
    } catch (error) {
      console.error('Failed to update profile:', error)
      toast.error('Failed to update profile')
    } finally {
      setLoading(false)
    }
  }

  const handleNotificationUpdate = async () => {
    setLoading(true)

    try {
      await apiClient.put('/api/users/notification-settings', notificationSettings)
      toast.success('Notification settings updated!')
    } catch (error) {
      console.error('Failed to update notification settings:', error)
      toast.error('Failed to update notification settings')
    } finally {
      setLoading(false)
    }
  }

  const handleScheduleUpdate = async () => {
    setLoading(true)

    try {
      await apiClient.put('/api/users/report-schedule', reportSchedule)
      toast.success('Report schedule updated!')
    } catch (error) {
      console.error('Failed to update report schedule:', error)
      toast.error('Failed to update report schedule')
    } finally {
      setLoading(false)
    }
  }

  const getRoleBadgeVariant = (role: string) => {
    switch (role) {
      case 'ADMIN':
        return 'destructive'
      case 'LEADER':
        return 'default'
      case 'MANAGER':
        return 'secondary'
      default:
        return 'outline'
    }
  }

  if (!user) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="animate-spin rounded-full h-32 w-32 border-b-2 border-blue-600"></div>
      </div>
    )
  }

  return (
    <div className="space-y-6">
      <div className="flex justify-between items-center">
        <div>
          <h1 className="text-3xl font-bold text-gray-900">User Profile</h1>
          <p className="text-gray-600">Manage your account settings and preferences</p>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <Card>
          <CardHeader>
            <CardTitle>Profile Overview</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="flex flex-col items-center space-y-4">
              <Avatar className="h-20 w-20">
                <AvatarFallback className="text-lg">
                  {user.firstName[0]}{user.lastName[0]}
                </AvatarFallback>
              </Avatar>
              <div className="text-center">
                <h3 className="text-lg font-medium">{user.firstName} {user.lastName}</h3>
                <p className="text-gray-600">{user.email}</p>
                <Badge variant={getRoleBadgeVariant(user.role)} className="mt-2">
                  {user.role}
                </Badge>
              </div>
              <div className="w-full space-y-2 text-sm">
                <div className="flex justify-between">
                  <span className="text-gray-600">Member since:</span>
                  <span>{new Date(user.createdAt).toLocaleDateString()}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-gray-600">Status:</span>
                  <Badge variant={user.isActive ? 'default' : 'secondary'}>
                    {user.isActive ? 'Active' : 'Inactive'}
                  </Badge>
                </div>
                {user.manager && (
                  <div className="flex justify-between">
                    <span className="text-gray-600">Manager:</span>
                    <span>{user.manager.firstName} {user.manager.lastName}</span>
                  </div>
                )}
              </div>
            </div>
          </CardContent>
        </Card>

        <Card className="lg:col-span-2">
          <CardHeader>
            <CardTitle>Account Settings</CardTitle>
            <CardDescription>
              Update your personal information and preferences
            </CardDescription>
          </CardHeader>
          <CardContent>
            <Tabs defaultValue="profile" className="space-y-4">
              <TabsList>
                <TabsTrigger value="profile">Profile</TabsTrigger>
                <TabsTrigger value="notifications">Notifications</TabsTrigger>
                <TabsTrigger value="reports">Report Schedule</TabsTrigger>
              </TabsList>

              <TabsContent value="profile" className="space-y-4">
                <form onSubmit={handleProfileUpdate} className="space-y-4">
                  <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                    <div className="space-y-2">
                      <Label htmlFor="firstName">First Name</Label>
                      <Input
                        id="firstName"
                        value={profileData.firstName}
                        onChange={(e) => setProfileData(prev => ({ ...prev, firstName: e.target.value }))}
                        required
                      />
                    </div>
                    <div className="space-y-2">
                      <Label htmlFor="lastName">Last Name</Label>
                      <Input
                        id="lastName"
                        value={profileData.lastName}
                        onChange={(e) => setProfileData(prev => ({ ...prev, lastName: e.target.value }))}
                        required
                      />
                    </div>
                  </div>
                  
                  <div className="space-y-2">
                    <Label htmlFor="email">Email</Label>
                    <Input
                      id="email"
                      type="email"
                      value={profileData.email}
                      onChange={(e) => setProfileData(prev => ({ ...prev, email: e.target.value }))}
                      required
                    />
                  </div>

                  <Button type="submit" disabled={loading}>
                    <Save className="mr-2 h-4 w-4" />
                    {loading ? 'Saving...' : 'Save Changes'}
                  </Button>
                </form>
              </TabsContent>

              <TabsContent value="notifications" className="space-y-4">
                <div className="space-y-6">
                  <div>
                    <h4 className="text-lg font-medium mb-4">Email Notifications</h4>
                    <div className="space-y-4">
                      <div className="flex items-center justify-between">
                        <div className="space-y-0.5">
                          <Label>Daily Reports</Label>
                          <p className="text-sm text-gray-600">
                            Receive daily work summaries via email
                          </p>
                        </div>
                        <Switch
                          checked={notificationSettings.dailyReports}
                          onCheckedChange={(checked) => 
                            setNotificationSettings(prev => ({ ...prev, dailyReports: checked }))
                          }
                        />
                      </div>

                      <div className="flex items-center justify-between">
                        <div className="space-y-0.5">
                          <Label>Weekly Reports</Label>
                          <p className="text-sm text-gray-600">
                            Receive weekly work summaries via email
                          </p>
                        </div>
                        <Switch
                          checked={notificationSettings.weeklyReports}
                          onCheckedChange={(checked) => 
                            setNotificationSettings(prev => ({ ...prev, weeklyReports: checked }))
                          }
                        />
                      </div>

                      <div className="flex items-center justify-between">
                        <div className="space-y-0.5">
                          <Label>Meeting Reminders</Label>
                          <p className="text-sm text-gray-600">
                            Get notified about upcoming meetings
                          </p>
                        </div>
                        <Switch
                          checked={notificationSettings.meetingReminders}
                          onCheckedChange={(checked) => 
                            setNotificationSettings(prev => ({ ...prev, meetingReminders: checked }))
                          }
                        />
                      </div>

                      <div className="flex items-center justify-between">
                        <div className="space-y-0.5">
                          <Label>Team Updates</Label>
                          <p className="text-sm text-gray-600">
                            Receive updates about your team's activities
                          </p>
                        </div>
                        <Switch
                          checked={notificationSettings.teamUpdates}
                          onCheckedChange={(checked) => 
                            setNotificationSettings(prev => ({ ...prev, teamUpdates: checked }))
                          }
                        />
                      </div>
                    </div>
                  </div>

                  <Button onClick={handleNotificationUpdate} disabled={loading}>
                    <Bell className="mr-2 h-4 w-4" />
                    {loading ? 'Saving...' : 'Save Notification Settings'}
                  </Button>
                </div>
              </TabsContent>

              <TabsContent value="reports" className="space-y-4">
                <div className="space-y-6">
                  <div>
                    <h4 className="text-lg font-medium mb-4">Report Schedule</h4>
                    <div className="space-y-4">
                      <div className="space-y-2">
                        <Label>Daily Report Time</Label>
                        <div className="flex items-center space-x-2">
                          <Clock className="h-4 w-4 text-gray-500" />
                          <Input
                            type="time"
                            value={reportSchedule.dailyTime}
                            onChange={(e) => 
                              setReportSchedule(prev => ({ ...prev, dailyTime: e.target.value }))
                            }
                            className="w-32"
                          />
                        </div>
                        <p className="text-sm text-gray-600">
                          Time when daily reports will be sent
                        </p>
                      </div>

                      <div className="space-y-2">
                        <Label>Weekly Report Day</Label>
                        <div className="flex items-center space-x-2">
                          <Calendar className="h-4 w-4 text-gray-500" />
                          <Select 
                            value={reportSchedule.weeklyDay} 
                            onValueChange={(value) => 
                              setReportSchedule(prev => ({ ...prev, weeklyDay: value }))
                            }
                          >
                            <SelectTrigger className="w-40">
                              <SelectValue />
                            </SelectTrigger>
                            <SelectContent>
                              <SelectItem value="monday">Monday</SelectItem>
                              <SelectItem value="tuesday">Tuesday</SelectItem>
                              <SelectItem value="wednesday">Wednesday</SelectItem>
                              <SelectItem value="thursday">Thursday</SelectItem>
                              <SelectItem value="friday">Friday</SelectItem>
                              <SelectItem value="saturday">Saturday</SelectItem>
                              <SelectItem value="sunday">Sunday</SelectItem>
                            </SelectContent>
                          </Select>
                        </div>
                      </div>

                      <div className="space-y-2">
                        <Label>Weekly Report Time</Label>
                        <div className="flex items-center space-x-2">
                          <Clock className="h-4 w-4 text-gray-500" />
                          <Input
                            type="time"
                            value={reportSchedule.weeklyTime}
                            onChange={(e) => 
                              setReportSchedule(prev => ({ ...prev, weeklyTime: e.target.value }))
                            }
                            className="w-32"
                          />
                        </div>
                        <p className="text-sm text-gray-600">
                          Time when weekly reports will be sent
                        </p>
                      </div>
                    </div>
                  </div>

                  <Button onClick={handleScheduleUpdate} disabled={loading}>
                    <Calendar className="mr-2 h-4 w-4" />
                    {loading ? 'Saving...' : 'Save Schedule Settings'}
                  </Button>
                </div>
              </TabsContent>
            </Tabs>
          </CardContent>
        </Card>
      </div>
    </div>
  )
}

export default UserProfile
